/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "ComputePipeline.h"
#include "ImageProcessor.h"
#include "Utils.h"
#include "VulkanResources.h"

#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <cmath>

namespace vlur {
    namespace {

        bool beginOneTimeCommandBuffer(VkCommandBuffer cmd) {
            const VkCommandBufferBeginInfo commandBufferBeginInfo = {
                    .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,
                    .pNext = nullptr,
                    .flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT,
                    .pInheritanceInfo = nullptr,
            };
            CALL_VK(vkBeginCommandBuffer, cmd, &commandBufferBeginInfo);
            return true;
        }

        bool endAndSubmitCommandBuffer(VkCommandBuffer cmd, VkQueue queue) {
            // End command buffer recording
            CALL_VK(vkEndCommandBuffer, cmd);

            // Submit queue
            const VkSubmitInfo submitDesc = {
                    .sType = VK_STRUCTURE_TYPE_SUBMIT_INFO,
                    .pNext = nullptr,
                    .waitSemaphoreCount = 0,
                    .pWaitSemaphores = nullptr,
                    .pWaitDstStageMask = nullptr,
                    .commandBufferCount = 1,
                    .pCommandBuffers = &cmd,
                    .signalSemaphoreCount = 0,
                    .pSignalSemaphores = nullptr,
            };
            CALL_VK(vkQueueSubmit, queue, 1, &submitDesc, VK_NULL_HANDLE);
            CALL_VK(vkQueueWaitIdle, queue);
            return true;
        }

        void recordImageCopyingCommand(VkCommandBuffer cmd, const Image &src, const Image &dst) {
            const VkImageCopy imageCopy = {
                    .srcSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
                    .srcOffset = {0, 0, 0},
                    .dstSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
                    .dstOffset = {0, 0, 0},
                    .extent = {src.width(), src.height(), 1},
            };
            vkCmdCopyImage(cmd, src.getImageHandle(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                           dst.getImageHandle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1,
                           &imageCopy);
        }

    }  // namespace

    std::unique_ptr<ImageProcessor> ImageProcessor::create(
            bool enableDebug,
            AAssetManager *assetManager
    ) {
        auto processor = std::make_unique<ImageProcessor>();
        const bool success = processor->initialize(enableDebug, assetManager);
        return success ? std::move(processor) : nullptr;
    }

    bool ImageProcessor::initialize(bool enableDebug, AAssetManager *assetManager) {
        // Create context
        printf("%c", enableDebug ? 1 : 0);
        mContext = VulkanContext::create(false);
        RET_CHECK(mContext != nullptr);
        // Allocate command buffer
        mCommandBuffer =
                std::make_unique<VulkanCommandBuffer>(mContext->device(), mContext->commandPool());
        const VkCommandBufferAllocateInfo commandBufferAllocateInfo = {
                .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
                .pNext = nullptr,
                .commandPool = mContext->commandPool(),
                .level = VK_COMMAND_BUFFER_LEVEL_PRIMARY,
                .commandBufferCount = 1,
        };
        CALL_VK(vkAllocateCommandBuffers, mContext->device(), &commandBufferAllocateInfo,
                mCommandBuffer->pHandle());

        // Create two compute pipelines for blur
        mBlurUniformBuffer = Buffer::create(
                mContext.get(), sizeof(mBlurData), VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        RET_CHECK(mBlurUniformBuffer != nullptr);
        mBlurHorizontalPipeline =
                ComputePipeline::create(mContext.get(), "shaders/BlurHorizontal.comp.spv",
                                        assetManager,
                                        sizeof(int32_t), /*useUniformBuffer=*/true);
        RET_CHECK(mBlurHorizontalPipeline != nullptr);
        mBlurVerticalPipeline =
                ComputePipeline::create(mContext.get(), "shaders/BlurVertical.comp.spv",
                                        assetManager,
                                        sizeof(int32_t), /*useUniformBuffer=*/true);
        RET_CHECK(mBlurVerticalPipeline != nullptr);
        return true;
    }

    bool ImageProcessor::configureInputAndOutput(
            JNIEnv *env,
            jobject inputBitmap,
            int id
    ) {
        // Create input image from bitmap
        mInputImages[id] = Image::createFromBitmap(mContext.get(), env, inputBitmap);
        RET_CHECK(mInputImages[id] != nullptr);
        LOGV("Input image width = %d, height = %d", mInputImages[id]->width(),
             mInputImages[id]->height());

        // Create intermediate image for blur
        mTempImages[id] = Image::createDeviceLocal(mContext.get(), mInputImages[id]->width(),
                                                   mInputImages[id]->height(),
                                                   VK_IMAGE_USAGE_STORAGE_BIT |
                                                   VK_IMAGE_USAGE_SAMPLED_BIT);
        RET_CHECK(mTempImages[id] != nullptr);

        // Create staging output image
        mStagingOutputImages[id] = Image::createDeviceLocal(mContext.get(),
                                                            mInputImages[id]->width(),
                                                            mInputImages[id]->height(),
                                                            VK_IMAGE_USAGE_STORAGE_BIT |
                                                            VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
        RET_CHECK(mStagingOutputImages[id] != nullptr);

        // Create output images backed by AHardwareBuffer
        const AHardwareBuffer_Desc ahwbDesc = {
                .width = mInputImages[id]->width(),
                .height = mInputImages[id]->height(),
                .layers = 1,
                .format = AHARDWAREBUFFER_FORMAT_R8G8B8A8_UNORM,
                .usage = AHARDWAREBUFFER_USAGE_CPU_READ_NEVER |
                         AHARDWAREBUFFER_USAGE_CPU_WRITE_NEVER |
                         AHARDWAREBUFFER_USAGE_GPU_SAMPLED_IMAGE,
                .stride = 0,
                .rfu0 = 0,
                .rfu1 = 0,
        };
        AHardwareBuffer *buffer = nullptr;
        RET_CHECK(AHardwareBuffer_allocate(&ahwbDesc, &buffer) == 0);
        mOutputImages[id] = Image::createFromAHardwareBuffer(mContext.get(), buffer);
        AHardwareBuffer_release(buffer);
        RET_CHECK(mOutputImages[id] != nullptr);
        return true;
    }

    bool ImageProcessor::blur(float radius, int outputIndex) {
        RET_CHECK(1.0f <= radius && radius <= 25.0f);

        // Calculate gaussian kernel, this is equivalent to ComputeGaussianWeights at
        // https://cs.android.com/android/platform/superproject/+/master:frameworks/rs/cpu_ref/rsCpuIntrinsicBlur.cpp;l=57
        constexpr float e = 2.718281828459045f;
        constexpr float pi = 3.1415926535897932f;
        float sigma = 0.4f * radius + 0.6f;
        float coeff1 = 1.0f / (std::sqrtf(2.0f * pi) * sigma);
        float coeff2 = -1.0f / (2.0f * sigma * sigma);
        int32_t iRadius = static_cast<int>(std::ceilf(radius));
        float normalizeFactor = 0.0f;
        for (int r = -iRadius; r <= iRadius; r++) {
            const float value = coeff1 * std::powf(e, coeff2 * static_cast<float>(r * r));
            mBlurData.kernel[r + iRadius] = value;
            normalizeFactor += value;
        }
        normalizeFactor = 1.0f / normalizeFactor;
        for (int r = -iRadius; r <= iRadius; r++) {
            mBlurData.kernel[r + iRadius] *= normalizeFactor;
        }
        RET_CHECK(mBlurUniformBuffer->copyFrom(&mBlurData));

        // Apply a two-pass blur algorithm: a horizontal blur kernel followed by a vertical
        // blur kernel. This is equivalent to, but more efficient than applying a 2D blur
        // filter in a single pass. The two-pass blur algorithm has two kernels, each of
        // time complexity O(iRadius), while the single-pass algorithm has only one kernel,
        // but the time complexity is O(iRadius^2).
        auto cmd = mCommandBuffer->handle();
        RET_CHECK(beginOneTimeCommandBuffer(cmd));

        // The temp image is used as an output storage image in the first pass.
        mTempImages[outputIndex]->recordLayoutTransitionBarrier(cmd,
                                                                VK_IMAGE_LAYOUT_GENERAL, /*preserveData=*/
                                                                false);

        // First pass: apply a horizontal gaussian blur.
        mBlurHorizontalPipeline->recordComputeCommands(cmd, &iRadius, *mInputImages[outputIndex],
                                                       *mTempImages[outputIndex],
                                                       mBlurUniformBuffer.get());

        // The temp image is used as an input sampled image in the second pass,
        // and the staging image is used as an output storage image.
        mTempImages[outputIndex]->recordLayoutTransitionBarrier(cmd,
                                                                VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        mStagingOutputImages[outputIndex]->recordLayoutTransitionBarrier(cmd,
                                                                         VK_IMAGE_LAYOUT_GENERAL,
                /*preserveData=*/false);

        // Second pass: apply a vertical gaussian blur.
        mBlurVerticalPipeline->recordComputeCommands(cmd, &iRadius, *mTempImages[outputIndex],
                                                     *mStagingOutputImages[outputIndex],
                                                     mBlurUniformBuffer.get());

        // Prepare for image copying from the staging image to the output image.
        mStagingOutputImages[outputIndex]->recordLayoutTransitionBarrier(cmd,
                                                                         VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);

        // Copy staging image to output image.
        recordImageCopyingCommand(cmd, *mStagingOutputImages[outputIndex],
                                  *mOutputImages[outputIndex]);

        // Submit to queue.
        RET_CHECK(endAndSubmitCommandBuffer(cmd, mContext->queue()));
        return true;
    }

}  // namespace vlur
