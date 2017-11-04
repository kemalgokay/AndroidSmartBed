package com.rootsoft.endareco.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rootsoft.endareco.R;


import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Kemal on 02/04/17.
 */


public class AsyncCompareTask extends AsyncTask<Void, Void, Void> {

   //Sabitler
    private static final String TAG = "image " + AsyncCompareTask.class.toString();

    //Parametreler
    private Context context;


    private Bitmap bmpObjToRecognize, bmpScene, bmpMatchedScene;
    private double minDistance, maxDistance;
    private Scalar RED = new Scalar(255,0,0);
    private Scalar GREEN = new Scalar(0,255,0);
    private int matchesFound;

    //----------------------------------------------
    //Yapılandırıcı

    public AsyncCompareTask(Context context) {
        this.context = context;
        this.minDistance = 100;
        this.maxDistance = 0;
        this.matchesFound = 0;
    }
//-------------------------------------------
    //Özellikler

    public Bitmap getObjToRecognize() {
        return bmpObjToRecognize;
    }

    public void setObjToRecognize(Bitmap bmpObjToRecognize) {
        this.bmpObjToRecognize = bmpObjToRecognize;
    }

    public Bitmap getScene() {
        return bmpScene;
    }

    public void setScene(Bitmap bmpScene) {
        this.bmpScene = bmpScene;
    }

    public double getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(double minDistance) {
        this.minDistance = minDistance;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected Void doInBackground(Void... arg0) {
        // TODO Auto-generated method stub
        detectObject();
        return null;
    }
//Sonuc ekranı-------------------------------------------------------------****
    @Override
    protected void onPostExecute(Void result) {
        try {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(
                    context);
            alertDialog.setTitle("Sonuc");
            alertDialog.setCancelable(true);
            LayoutInflater factory = LayoutInflater.from(context);
            final View view = factory.inflate(R.layout.view_image_result, null);
            ImageView matchedImages = (ImageView) view
                    .findViewById(R.id.finalImage);
            matchedImages.setImageBitmap(bmpMatchedScene);
            matchedImages.invalidate();
            TextView message = (TextView) view.findViewById(R.id.message);
            //message.setText("Matches found: " + matchesFound);

            if(matchesFound >50){
                message.setText("Düzgündür.  "+matchesFound);
                alertDialog.setView(view);
            }else{
                message.setText("Düzgün değildir.  "+matchesFound);
                alertDialog.setView(view);
            }
            //message.setText("Aradaki fark oranı:  "+matchesFound);
            //alertDialog.setView(view);

            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.toString(),
                    Toast.LENGTH_LONG).show();
        }
    }
    //--------------------------------------------------------******
    //Obje algılama

    public void detectObject() {
        //Bildirimleri- deklerasyonları
        Mat mObjectMat = new Mat();
        Mat mSceneMat = new Mat();
        MatOfDMatch matches = new MatOfDMatch();
        List<DMatch> matchesList;
        LinkedList<DMatch> good_matches = new LinkedList<>();
        MatOfDMatch gm = new MatOfDMatch();
        LinkedList<Point> objList = new LinkedList<>();
        LinkedList<Point> sceneList = new LinkedList<>();
        MatOfPoint2f obj = new MatOfPoint2f();
        MatOfPoint2f scene = new MatOfPoint2f();

        MatOfKeyPoint keypoints_object = new MatOfKeyPoint();
        MatOfKeyPoint keypoints_scene = new MatOfKeyPoint();
        Mat descriptors_object = new Mat();
        Mat descriptors_scene = new Mat();

        //Bitmap to Mat
        Utils.bitmapToMat(bmpObjToRecognize, mObjectMat);
        Utils.bitmapToMat(bmpScene, mSceneMat);
        Mat img3 = mSceneMat.clone();

        //görüntüdeki feature point noktalarını bulmak için FeatureDetectr kullanıldı.
        FeatureDetector fd = FeatureDetector.create(FeatureDetector.ORB);
        fd.detect(mObjectMat, keypoints_object );
        fd.detect(mSceneMat, keypoints_scene );

        //DescriptorExtractor
       //key point için bir tanımlayıcı kılan bir algoritmadır.Bu algoritmadaki tanımlayıcılar SIFT gibi feature pointler kullanıldı.
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        extractor.compute(mObjectMat, keypoints_object, descriptors_object );
        extractor.compute(mSceneMat, keypoints_scene, descriptors_scene );

        //DescriptorMatcher
        //yukarı da kullanılanların eşleşmesi için.
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        matcher.match( descriptors_object, descriptors_scene, matches);

        //keypointler arasında ki max ve min değerleini hesaplaması için.
        matchesList = matches.toList();
        for( int i = 0; i < descriptors_object.rows(); i++ )
        {
            Double dist = (double) matchesList.get(i).distance;
            if( dist < minDistance ) minDistance = dist;
            if( dist > maxDistance ) maxDistance = dist;
        }

        ////Draw only good matches
        for(int i = 0; i < descriptors_object.rows(); i++){
            if(matchesList.get(i).distance < 3*minDistance){
                good_matches.addLast(matchesList.get(i));
            }
        }
        gm.fromList(good_matches);
        matchesFound = good_matches.size();

        //Draw the matches
        Features2d.drawMatches(mObjectMat, keypoints_object, mSceneMat, keypoints_scene, gm, img3);

        //Eşleşmelerdeki önemli noktalar
        List<KeyPoint> keypoints_objectList = keypoints_object.toList();
        List<KeyPoint> keypoints_sceneList = keypoints_scene.toList();

        for(int i = 0; i<good_matches.size(); i++){
            objList.addLast(keypoints_objectList.get(good_matches.get(i).queryIdx).pt);
            sceneList.addLast(keypoints_sceneList.get(good_matches.get(i).trainIdx).pt);
        }

        obj.fromList(objList);
        scene.fromList(sceneList);

        //kamera ekranı ve nesneler arasındaki eş değerli ve eş özellikli olan degerlei bulmak icin.
        //Kalibrasyon değerlerini ayarlamak yapmak için.
        Mat hg = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, minDistance);

        // mObjectToDetectMat yardımıyla köşelerin alınması.
        Mat obj_corners = new Mat(4,1, CvType.CV_32FC2);
        Mat scene_corners = new Mat(4,1,CvType.CV_32FC2);

        obj_corners.put(0, 0, new double[] {0,0});
        obj_corners.put(1, 0, new double[] {mObjectMat.cols(),0});
        obj_corners.put(2, 0, new double[] {mObjectMat.cols(),mObjectMat.rows()});
        obj_corners.put(3, 0, new double[] {0,mObjectMat.rows()});

        Core.perspectiveTransform(obj_corners, scene_corners, hg);

        Core.line(img3, new Point(scene_corners.get(0,0)), new Point(scene_corners.get(1,0)), new Scalar(0, 255, 0),4);
        Core.line(img3, new Point(scene_corners.get(1,0)), new Point(scene_corners.get(2,0)), new Scalar(0, 255, 0),4);
        Core.line(img3, new Point(scene_corners.get(2,0)), new Point(scene_corners.get(3,0)), new Scalar(0, 255, 0),4);
        Core.line(img3, new Point(scene_corners.get(3,0)), new Point(scene_corners.get(0,0)), new Scalar(0, 255, 0),4);


        //Mat To Bitmap dönüşümü
        try {
            bmpMatchedScene = Bitmap.createBitmap(img3.cols(), img3.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img3, bmpMatchedScene);
        }
        catch (CvException e){Log.d("Exception",e.getMessage());}
    }

    //-----------------------------------------------------------------------------------
}
