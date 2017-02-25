package edu.byu.rvl.myvisiondriveapp;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class hsv_thresholder {
    public hsv_thresholder () {
    }
    private static final int H_MIN_INDEX = 0;
    private static final int H_MAX_INDEX = 1;
    private static final int S_MIN_INDEX = 2;
    private static final int S_MAX_INDEX = 3;
    private static final int V_MIN_INDEX = 4;
    private static final int V_MAX_INDEX = 5;
    private Mat HSV_mat;
    private Mat H_mat;
    private Mat S_mat;
    private Mat V_mat;
    private Mat H_thresh_mat;
    private Mat S_thresh_mat;
    private Mat V_thresh_mat;
    private Mat temp1_mat;
    private Mat temp2_mat;
    private Mat final_thresh_mat;
    public void init(){
        HSV_mat = new Mat();
        H_mat = new Mat();
        S_mat = new Mat();
        V_mat = new Mat();
        H_thresh_mat = new Mat();
        S_thresh_mat = new Mat();
        V_thresh_mat = new Mat();
        temp1_mat = new Mat();
        temp2_mat = new Mat();
        final_thresh_mat = new Mat();
    }

    public Mat get_thresh_image(int[] threshold){
        apply_threshold(threshold);
//        combine_thresholds();
        return H_thresh_mat;
    }
    private void apply_threshold(int[] threshold){
        threshold_within_range(H_mat,threshold[H_MIN_INDEX],threshold[H_MAX_INDEX],H_thresh_mat);
//        threshold_within_range(S_mat,threshold[S_MIN_INDEX],threshold[S_MAX_INDEX],S_thresh_mat);
//        threshold_within_range(V_mat,threshold[V_MIN_INDEX],threshold[V_MAX_INDEX],V_thresh_mat);
    }
    private void combine_thresholds(){
//        temp1_mat.empty();
        Core.bitwise_and(H_thresh_mat,S_thresh_mat,temp1_mat);
        Core.bitwise_and(temp1_mat,V_thresh_mat,final_thresh_mat);
    }
    private void threshold_within_range(Mat mat, int min, int max,Mat dst){
        double max_value = Core.minMaxLoc(mat).maxVal;
        double min_value = Core.minMaxLoc(mat).minVal;
        if(min > 0) {
        Imgproc.threshold(mat,temp1_mat,max,max_value,Imgproc.THRESH_TOZERO_INV);
        Imgproc.threshold(temp1_mat,dst,min,max_value,Imgproc.THRESH_BINARY);
        }
        else {
            Imgproc.threshold(mat, dst, max, max_value, Imgproc.THRESH_BINARY_INV);
//        Imgproc.threshold(temp1_mat,dst,min,max_value,Imgproc.THRESH_BINARY);
        }

    }
    private void extract_HSV_channels(){
        Core.extractChannel(HSV_mat, H_mat, 0);         // hue
        Core.extractChannel(HSV_mat, S_mat, 1);         // hue
        Core.extractChannel(HSV_mat, V_mat, 2);         // hue
    }
    public void read_RGB_image(Mat rgb_mat){
        Imgproc.cvtColor(rgb_mat, HSV_mat, Imgproc.COLOR_RGB2HSV);
        extract_HSV_channels();
    }
}
