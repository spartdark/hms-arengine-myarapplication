/**
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

package com.vsm.myarapplication.body.rendering;

import android.opengl.GLES20;

import com.huawei.hiar.ARBody;
import com.huawei.hiar.ARCoordinateSystemType;
import com.huawei.hiar.ARTrackable;
import com.vsm.myarapplication.common.ShaderUtil;

import java.nio.FloatBuffer;
import java.util.Collection;

/**
 * Obtain and pass the skeleton data to openGL ES, which will render the data and displays it on the screen.
 *
 * @author HW
 * @since 2020-03-27
 */
public class BodySkeletonDisplay implements BodyRelatedDisplay {
    private static final String TAG = BodySkeletonDisplay.class.getSimpleName();

    // Number of bytes occupied by each 3D coordinate. Float data occupies 4 bytes.
    // Each skeleton point represents a 3D coordinate.
    private static final int BYTES_PER_POINT = 4 * 3;

    private static final int INITIAL_POINTS_SIZE = 150;

    private static final float DRAW_COORDINATE = 2.0f;

    private int mVbo;

    private int mVboSize;

    private int mProgram;

    private int mPosition;

    private int mProjectionMatrix;

    private int mColor;

    private int mPointSize;

    private int mCoordinateSystem;

    private int mNumPoints = 0;

    private int mPointsNum = 0;

    private FloatBuffer mSkeletonPoints;

    /**
     * Create a body skeleton shader on the GL thread.
     * This method is called when {@link BodyRenderManager#onSurfaceCreated}.
     */
    @Override
    public void init() {
        ShaderUtil.checkGlError(TAG, "Init start.");
        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        mVbo = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);

        mVboSize = INITIAL_POINTS_SIZE * BYTES_PER_POINT;
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ShaderUtil.checkGlError(TAG, "Before create gl program.");
        createProgram();
        ShaderUtil.checkGlError(TAG, "Init end.");
    }

    private void createProgram() {
        ShaderUtil.checkGlError(TAG, "Create gl program start.");
        mProgram = BodyShaderUtil.createGlProgram();
        mColor = GLES20.glGetUniformLocation(mProgram, "inColor");
        mPosition = GLES20.glGetAttribLocation(mProgram, "inPosition");
        mPointSize = GLES20.glGetUniformLocation(mProgram, "inPointSize");
        mProjectionMatrix = GLES20.glGetUniformLocation(mProgram, "inProjectionMatrix");
        mCoordinateSystem = GLES20.glGetUniformLocation(mProgram, "inCoordinateSystem");
        ShaderUtil.checkGlError(TAG, "Create gl program end.");
    }

    private void updateBodySkeleton() {
        ShaderUtil.checkGlError(TAG, "Update Body Skeleton data start.");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);
        mNumPoints = mPointsNum;

        if (mVboSize < mNumPoints * BYTES_PER_POINT) {
            while (mVboSize < mNumPoints * BYTES_PER_POINT) {
                // If the size of VBO is insufficient to accommodate the new point cloud, resize the VBO.
                mVboSize *= 2;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, mNumPoints * BYTES_PER_POINT, mSkeletonPoints);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGlError(TAG, "Update Body Skeleton data end.");
    }

    /**
     * Update the node data and draw by using OpenGL.
     * This method is called when {@link BodyRenderManager#onDrawFrame}.
     *
     * @param bodies Body data.
     * @param projectionMatrix projection matrix.
     */
    @Override
    public void onDrawFrame(Collection<ARBody> bodies, float[] projectionMatrix) {
        for (ARBody body : bodies) {
            if (body.getTrackingState() == ARTrackable.TrackingState.TRACKING) {
                float coordinate = 1.0f;
                if (body.getCoordinateSystemType() == ARCoordinateSystemType.COORDINATE_SYSTEM_TYPE_3D_CAMERA) {
                    coordinate = DRAW_COORDINATE;
                }
                findValidSkeletonPoints(body);
                updateBodySkeleton();
                drawBodySkeleton(coordinate, projectionMatrix);
            }
        }
    }

    private void drawBodySkeleton(float coordinate, float[] projectionMatrix) {
        ShaderUtil.checkGlError(TAG, "Draw body skeleton start.");

        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(mPosition);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo);

        // The size of the vertex attribute is 4, and each vertex has four coordinate components.
        GLES20.glVertexAttribPointer(
                mPosition, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniform4f(mColor, 0.0f, 0.0f, 1.0f, 1.0f);
        GLES20.glUniformMatrix4fv(mProjectionMatrix, 1, false, projectionMatrix, 0);

        // Set the size of the skeleton points.
        GLES20.glUniform1f(mPointSize, 30.0f);
        GLES20.glUniform1f(mCoordinateSystem, coordinate);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, mNumPoints);
        GLES20.glDisableVertexAttribArray(mPosition);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        ShaderUtil.checkGlError(TAG, "Draw body skeleton end.");
    }

    private void findValidSkeletonPoints(ARBody arBody) {
        int index = 0;
        int[] isExists;
        int validPointNum = 0;
        float[] points;
        float[] skeletonPoints;

        // Determine whether the data returned by the algorithm is 3D human
        // skeleton data or 2D human skeleton data, and obtain valid skeleton points.
        if (arBody.getCoordinateSystemType() == ARCoordinateSystemType.COORDINATE_SYSTEM_TYPE_3D_CAMERA) {
            isExists = arBody.getSkeletonPointIsExist3D();
            points = new float[isExists.length * 3];
            skeletonPoints = arBody.getSkeletonPoint3D();
        } else {
            isExists = arBody.getSkeletonPointIsExist2D();
            points = new float[isExists.length * 3];
            skeletonPoints = arBody.getSkeletonPoint2D();
        }

        // Save the three coordinates of each joint point(each point has three coordinates).
        for (int i = 0; i < isExists.length; i++) {
            if (isExists[i] != 0) {
                points[index++] = skeletonPoints[3 * i];
                points[index++] = skeletonPoints[3 * i + 1];
                points[index++] = skeletonPoints[3 * i + 2];
                validPointNum++;
            }
        }
        mSkeletonPoints = FloatBuffer.wrap(points);
        mPointsNum = validPointNum;
    }
}