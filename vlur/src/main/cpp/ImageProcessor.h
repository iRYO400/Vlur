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

#ifndef VLUR_IMAGE_PROCESSOR_H
#define VLUR_IMAGE_PROCESSOR_H

#include "ComputePipeline.h"
#include "VulkanContext.h"
#include "VulkanResources.h"
#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <cmath>
#include <jni.h>
#include <memory>
#include <unordered_map>

namespace vlur {

    class ImageProcessor {
    public:
        // Create an image processor and initialize compute pipelines. If enableDebug is true,
        // the Vulkan instance will be created with the validation layer "VK_LAYER_KHRONOS_validation".
        // Return the created ImageProcessor on success, or nullptr if failed.
        static std::unique_ptr<ImageProcessor>
        create(bool enableDebug, AAssetManager *assetManager);

        // Prefer ImageProcessor::create
        ImageProcessor() = default;

        // Create the input image from bitmap and allocate output images backed by AHardwareBuffers.
        bool configureInputAndOutput(JNIEnv *env, jobject inputBitmap, int id);

        // Get the managed AHardwareBuffer of the target output.
        AHardwareBuffer *getOutputAHardwareBuffer(int id) {
            return mOutputImages[id]->getAHardwareBuffer();
        }

        bool blur(float radius, int outputIndex);

    private:
        // Return true on success, false if initialization failed.
        bool initialize(bool enableDebug, AAssetManager *assetManager);

        // Context
        std::unique_ptr<VulkanContext> mContext;

        // Images
        std::unordered_map<int, std::unique_ptr<Image>> mOutputImages;
        std::unordered_map<int, std::unique_ptr<Image>> mInputImages;
        std::unordered_map<int, std::unique_ptr<Image>> mTempImages;
        std::unordered_map<int, std::unique_ptr<Image>> mStagingOutputImages;

        // Command buffer
        std::unique_ptr<VulkanCommandBuffer> mCommandBuffer;

        // Compute pipelines and uniform buffer for blur
        struct {
            // A float array of length 52.
            float kernel[52] = {};
        } mBlurData;
        std::unique_ptr<Buffer> mBlurUniformBuffer;
        std::unique_ptr<ComputePipeline> mBlurHorizontalPipeline;
        std::unique_ptr<ComputePipeline> mBlurVerticalPipeline;
    };
}

#endif  // VLUR_IMAGE_PROCESSOR_H
