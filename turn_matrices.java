package edu.byu.rvl.myvisiondriveapp;

public class turn_matrices {
    public static final int CONE_MATRIX = 0;
    public static final int TUBE_MATRIX = 1;
    private static final int[][] edge_border_matrix = {
            {0,0,0,0,0,0,0,0,0,0},
            {0,0,0,-1,0,0,1,0,0,0},
            {0,0,0,-2,0,0,2,0,0,0},
            {0,0,-1,-3,0,0,3,1,0,0},
            {1,-1,-2,-4,0,0,4,2,1,1},
            {-2,-3,-3,-5,0,0,5,3,3,2},
            {-4,-5,-5,-7,0,0,7,5,5,4},
        {-5,-5,-7,-8,0,0,8,7,5,5},
            {-7,-7,-8,-9,0,0,9,8,7,7},
            {-7,-7,-8,-9,0,0,9,8,7,7}};

    private static final int num_of_rows = 10;
    private static final int num_of_cols = 10;
    public turn_matrices(){

    }
    public int[][] get_border_turn_matrix(){
//        switch (which_matrix){
//            case CONE_MATRIX:
//                return cone_matrix;
//            case TUBE_MATRIX:
//                return edge_border_matrix;
//            default:
//                return edge_border_matrix;
//        }
        return edge_border_matrix;
    }
    public int get_num_of_rows(){
        return num_of_rows;
    }
    public int get_num_of_cols(){
        return num_of_cols;
    }
}
