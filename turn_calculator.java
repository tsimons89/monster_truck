package edu.byu.rvl.myvisiondriveapp;
import org.opencv.core.Mat;

/**
 * Created by taylo on 9/19/2016.
 */
public class turn_calculator {
    private object_grid_generator grid_generator;
    private turn_matrices turn_mats;
    private int grid_rows;
    private int grid_cols;
    public int prefered_direction; //Left is negative
    private boolean use_prefered_direction;
    public boolean front_blocked = false;
    private final static int side_blocked_height = 7;
    private final static int front_blocked_width = 4;
    private final static int front_blocked_height = 8;
    public int num_free_right_cols;
    public int num_free_left_cols;

    public void init(double grid_percent_thresh){
        grid_generator = new object_grid_generator();
        turn_mats = new turn_matrices();
        grid_generator.initialize(turn_mats.get_num_of_rows(),turn_mats.get_num_of_cols(),grid_percent_thresh);
        grid_rows = turn_mats.get_num_of_rows();
        grid_cols = turn_mats.get_num_of_cols();
    }
    public int calculate_turn(Mat image,Mat display){
        int [][] object_grid = grid_generator.generate_object_grid(image,display);
        set_prefered_direction(object_grid);
        int max_turn_value = 0;
        if(use_prefered_direction){
            if(prefered_direction > 0 ){
                return -10;
            }
            return 10;
        }
        for(int row = 0; row < grid_rows; row++){
            for(int col = 0; col < grid_cols; col++){
                if(object_grid[row][col] > 0){
                    if(Math.abs(turn_mats.get_border_turn_matrix()[row][col]) > Math.abs(max_turn_value)){
                        max_turn_value = turn_mats.get_border_turn_matrix()[row][col];
                    }
                }
            }
        }
        return max_turn_value;
    }
    public void set_blocked(int [] col_score){
        num_free_right_cols = 0;
        num_free_left_cols = 0;
        front_blocked = false;
        int front_blocked_count = 0;
        for(int i = 0;i < grid_cols;i ++ ){
            if(col_score[i] <= side_blocked_height){
                num_free_left_cols++;
            }
            else{
                break;
            }
        }
        for(int i = grid_cols-1;i >=0 ;i -- ){
            if(col_score[i] <= side_blocked_height){
                num_free_right_cols++;
            }
            else{
                break;
            }
        }
        for(int i = 0;i < front_blocked_width;i ++ ){
            if(col_score[i + (grid_cols-front_blocked_width)/2] >= front_blocked_height){
                front_blocked_count++;
            }
        }
        front_blocked = (front_blocked_count >= front_blocked_width - 1);


    }
    public void set_prefered_direction(int [][] object_grid) {
        int [] col_score = new int[grid_cols];
        for (int col = 0; col < grid_cols; col++) {
            col_score[col] = -1;
            for (int row = grid_rows - 1; row >= 0; row--) {
                if(object_grid[row][col] > 0){
                    col_score[col] = row;
                    break;
                }
            }
        }
        set_blocked(col_score);
        if(is_blocked(col_score)){
            int direction = diff_of_col_score_sides(col_score);
            if(Math.abs(direction) > 1){
                prefered_direction = direction;
                use_prefered_direction = false;
            }
            else{
                use_prefered_direction = true;
            }
        }
        else{
            use_prefered_direction = false;
        }
    }
    private boolean is_blocked(int [] col_score){
        for (int col = 0; col < grid_cols; col++) {
            if(col_score[col] == -1)
                return false;
        }
        return true;
    }
    private int diff_of_col_score_sides(int [] col_score){//Turn left if negative and right if positive
        return col_score[0] - col_score[grid_cols-1];
    }
}
