package edu.byu.rvl.myvisiondriveapp;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;



public class object_grid_generator {

    private int [][] grid;
    private int grid_width;
    private int grid_height;
    private Mat cell;
    private  Rect cell_rect;
    private double count_thresh;
    private double percent_thresh;
    public object_grid_generator(){
    }
    public void initialize(int grid_width,int grid_height,double percent_thresh){
        this.grid_height = grid_height;
        this.grid_width = grid_width;
        grid = new int[grid_width][grid_height];
        cell = new Mat();
        cell_rect = new Rect();
        this.percent_thresh = percent_thresh;
    }

    public int[][] generate_object_grid(Mat src,Mat disp){
        set_cell_dimensions(src);
        for(int row = 0; row < grid_height;row++){
            cell_rect.y = row*cell_rect.height;
            for(int col = 0; col < grid_width;col++) {
                set_cell_count(src, row, col);
                if (grid[row][col] > 0) {
                    Core.rectangle(disp, new Point(cell_rect.x, cell_rect.y), new Point((cell_rect.x + cell_rect.width - 1), (cell_rect.y + cell_rect.height - 1)), new Scalar(255, 255, 0, 0));
                }
            }
        }
        return grid;
    }
    public int[][] generate_object_grid(Mat src){
        set_cell_dimensions(src);
        for(int row = 0; row < grid_height;row++){
            cell_rect.y = row*cell_rect.height;
            for(int col = 0; col < grid_width;col++) {
                set_cell_count(src, row, col);
            }
        }
        return grid;
    }

    private void set_cell_count(Mat src, int row, int col) {
        cell_rect.x = col * cell_rect.width;
        cell = src.submat(cell_rect);
        if(Core.countNonZero(cell) > count_thresh) {
            grid[row][col] = 1;
        }
        else{
            grid[row][col] = 0;
        }
    }

    private void set_cell_dimensions(Mat src){
        cell_rect.width = src.width()/grid_width;
        cell_rect.height = src.height()/grid_height;
        count_thresh  = cell_rect.width*cell_rect.height*percent_thresh;
    }
}
