package com.example.opencvlib;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

/**
 * @author yeqing
 * @des 判读图片清晰度
 * @date 2021/7/16 9:53
 */
public class OpencvUtil {
    private static String TAG = "opencv";

    public static boolean isBlurByOpenCV(String picFilePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // 通过path得到一个不超过2000*2000的Bitmap
        Bitmap image = decodeSampledBitmapFromFile(picFilePath, options, 2000, 2000);
        return isBlurByOpenCV(image);
    }

    public static boolean isBlurByOpenCV(Bitmap image) {
        if (image != null) {
            try {
                Log.d(TAG, "image.w=" + image.getWidth() + ",image.h=" + image.getHeight());
                int l = CvType.CV_8UC1; //8-bit grey scale image
                Mat matImage = new Mat();
                Utils.bitmapToMat(image, matImage);
//                Mat matImageGrey = new Mat();
                Imgproc.cvtColor(matImage, matImage, Imgproc.COLOR_BGR2GRAY); // 图像灰度化

                try {
                    Bitmap destImage = Bitmap.createBitmap(image);
                    Mat dst2 = new Mat();
                    Utils.bitmapToMat(destImage, dst2);
                    Mat laplacianImage = new Mat();
                    dst2.convertTo(laplacianImage, l);
                    dst2.release();
                    Imgproc.Laplacian(matImage, laplacianImage, CvType.CV_8U); // 拉普拉斯变换
                    Mat laplacianImage8bit = new Mat();
                    laplacianImage.convertTo(laplacianImage8bit, l);
                    laplacianImage.release();
                    //bitmap回收
                    if (destImage != null && !destImage.isRecycled()) {
                        destImage.recycle();
                        destImage = null;
                    }

                    Bitmap bmp = Bitmap.createBitmap(laplacianImage8bit.cols(), laplacianImage8bit.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(laplacianImage8bit, bmp);
                    int[] pixels = new int[bmp.getHeight() * bmp.getWidth()];
                    bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight()); // bmp为轮廓图

                    int maxLap = -16777216; // 16m
                    for (int pixel : pixels) {
                        if (pixel > maxLap)
                            maxLap = pixel;
                    }
                    int userOffset = -3881250; // 界线（严格性）降低一点
                    int soglia = -6118750 + userOffset; // -6118750为广泛使用的经验值
                    System.out.println("maxLap=" + maxLap);
                    if (maxLap <= soglia) {
                        System.out.println("这是一张模糊图片");
                    }
                    //bitmap回收
                    if (bmp != null && !bmp.isRecycled()) {
                        bmp.recycle();
                        bmp = null;
                        System.gc();
                    }
                    soglia += 6118750 + userOffset;
                    maxLap += 6118750 + userOffset;

                    Log.d(TAG, "opencvanswers..result：image.w=" + image.getWidth() + ", image.h=" + image.getHeight()
                            + "\nmaxLap= " + maxLap + "(清晰范围:0~" + (6118750 + userOffset) + ")"
                            + "\n" + Html.fromHtml("<font color='#eb5151'><b>" + (maxLap <= soglia ? "模糊" : "清晰") + "</b></font>"));
                    return maxLap <= soglia;
                } catch (OutOfMemoryError e) {
                    return false;
                }

            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public static boolean isBlur(String picFilePath) {
        Bitmap image = BitmapFactory.decodeFile(picFilePath);
        return isBlur(image);
    }

    public static boolean isBlur(Bitmap bitmap) {
        try {
            Mat matImage = new Mat();
            Mat matGray = new Mat();
            Utils.bitmapToMat(bitmap, matImage);
            Imgproc.cvtColor(matImage, matGray, Imgproc.COLOR_BGR2GRAY); // 图像灰度化
            Mat laplacianImage = new Mat();
            Imgproc.Laplacian(matGray, laplacianImage, CvType.CV_64F); // 拉普拉斯变换
            //求灰度图像的标准差
            MatOfDouble meanValueImage = new MatOfDouble();
            MatOfDouble meanStdValueImage = new MatOfDouble();
            Core.meanStdDev(matGray, meanValueImage, meanStdValueImage);
            double meanValue = 0.0;
            meanValue = meanStdValueImage.get(0, 0)[0];
            Log.d("yeqing", "图像的标准差--" + meanValue);
            if (meanValue < 10) {
                return true;
            }
        }catch (Exception e){
            return false;
        }

        return false;
    }

    public static Bitmap decodeSampledBitmapFromFile(String imgPath, BitmapFactory.Options options, int reqWidth, int reqHeight) {
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imgPath, options);
        // inSampleSize为缩放比例，举例：options.inSampleSize = 2表示缩小为原来的1/2，3则是1/3，以此类推
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imgPath, options);
    }


    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        while ((height / inSampleSize) > reqHeight || (width / inSampleSize) > reqWidth) {
            inSampleSize *= 2;
        }
        Log.d(TAG, "inSampleSize=" + inSampleSize);
        return inSampleSize;
    }
}
