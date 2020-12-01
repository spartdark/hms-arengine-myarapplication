package com.vsm.myarapplication.hand.rendering; /**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.huawei.hiar.ARHand;

import java.util.Collection;

/**
 * Rendering hand AR type related data.
 *
 * @author HW
 * @since 2020-05-22
 */
interface HandRelatedDisplay {
    /**
     * Init render.
     */
    void init();

    /**
     * Render objects, call per frame
     *
     * @param hands ARHands
     * @param projectionMatrix Camera projection matrix.
     */
    void onDrawFrame(Collection<ARHand> hands, float[] projectionMatrix);
}