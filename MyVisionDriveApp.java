package edu.byu.rvl.myvisiondriveapp;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import java.util.Calendar;


public class MyVisionDriveApp extends IOIOActivity implements View.OnTouchListener, CvCameraViewListener2 {
    private CameraBridgeViewBase mOpenCvCameraView;
    static final int 				N_BUFFERS = 2;
    static final int				NUM_FINGERS = 2;
    public static int           	viewMode = 0;
    public static int[]			    TouchX, TouchY;
    public static float				StartX, StartY;
    public static int				actionCode;
    public static int				pointerCount = 0;
    public static int				inputValueX = 0;
    public static int				inputValueY = 0;
    public static int[]             input_min = {0,0,0};
    public static int[]             input_max = {0,0,0};
    public static int               turn_value = 0;

    private ArrayList<String> MenuItems = new ArrayList<String>();
    Mat mRgba[];
    Mat mHSV;
    Mat mChannel;
    Mat mDisplay;
    Mat smallRgba;
    int	bufferIndex;
    int FrameHeight;
    int FrameWidth;
    public static final int [] cone_thresh = {-1,13,150,255,100,255};
    public static final int [] tube_thresh = {100,130,80,150,200,255};
    public boolean drive = false;

    //PID controller
    private static final double K_P = 1;
    private static final double K_I = .1;
    private static final double K_D = .1;
    private double error_prior = 0;
    private double integral = 0;
    private double desired_speed_freq;
    private static final int BACKUP_FREQ = 1200;
    private static final int BACKUP_DURATION = 8;
    private static final int BACKUP_TURN_VALUE = 9;
    private double freq;
    private static final int IOIO_POWER_MAX = 200;
    private int pid_direction = 1;
    // IOIO Control
    boolean LED = false;
    int     Frequency;
    int     SteerOutput;
    int     PowerOutput;
    boolean IOIO_Setup = false;

    private long prev_sec = 0;

    public static final int  STEER_MAX = 1000;
    public static final int  POWER_MAX = 2000;
    public static final int  STEER_OFF = 1000;
    public static final int  POWER_OFF = 500;
    public static final double down_samp_scale = 4.0;

    public static final hsv_thresholder thresholder = new hsv_thresholder();
    public static final object_grid_generator grid_generator = new object_grid_generator();
    public static final double grid_percent_thresh = .3;
    public static final turn_matrices turn_mats = new turn_matrices();
    public static final turn_calculator tube_turn_calc = new turn_calculator();
    public static final turn_calculator cone_turn_calc = new turn_calculator();

    private boolean left_blocked;
    private boolean right_blocked;
    private boolean backup_left = false;
    private boolean backup_right = false;
    private int backup_count;
    public enum Backup_State{
        FIRST_STOP,OFF,FIRST_TURN,STRIGHT,SECOND_TURN,SECOND_STOP
    }
    Backup_State backup_state = Backup_State.OFF;
    public static final int BACKUP_SPEED  = -150;


    public Size zero_size;
    private int pid_results;




    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MyVisionDriveApp.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase) new JavaCameraView(this, -1);
        setContentView(mOpenCvCameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();
//        mOpenCvCameraView.setMaxFrameSize(720,480);

    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }

    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        int i;
        TouchX = new int[NUM_FINGERS];
        TouchY = new int[NUM_FINGERS];
        inputValueX = 0;
        inputValueY = 0;
        bufferIndex = 0;
        FrameHeight = height;
        FrameWidth = width;

        mRgba = new Mat[N_BUFFERS];
        for (i=0; i<N_BUFFERS; i++) {
            mRgba[i]= new Mat(FrameHeight, FrameWidth, CvType.CV_8UC4);
        }
        zero_size = new Size(0.0, 0.0);
        mDisplay= new Mat();
        mHSV= new Mat();
        mChannel = new Mat();
        smallRgba = new Mat();
//        MenuItems.add("Original");
//        MenuItems.add("Drive");
//        MenuItems.add("Cone");
//        MenuItems.add("Tube");
        thresholder.init();
        tube_turn_calc.init(grid_percent_thresh);
        cone_turn_calc.init(grid_percent_thresh);

    }

    public void onCameraViewStopped() {

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_vision_drive, menu);

//        menu.add(MenuItems.get(0));
//        menu.add(MenuItems.get(1));
//        menu.add(MenuItems.get(2));
//        menu.add(MenuItems.get(3));
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        viewMode = MenuItems.indexOf(item.toString());
        mDisplay= new Mat(FrameHeight, FrameWidth, CvType.CV_8UC4);

        return super.onOptionsItemSelected(item);
    }

    public boolean onTouch(View v, MotionEvent event) {
        int i;
        pointerCount = event.getPointerCount();
        actionCode = event.getAction();
        if (actionCode == MotionEvent.ACTION_DOWN) {                                // get the starting location from the first touch
            StartX = event.getX(0);
            StartY = event.getY(0);
            for (i = 0; i < pointerCount && i < NUM_FINGERS; i++) {                        // get locations for up to to 5 touches
                TouchX[i] = (int) event.getX(i);
                TouchY[i] = (int) event.getY(i);
            }
        } else if (actionCode == MotionEvent.ACTION_MOVE) {
            for (i = 0; i < pointerCount && i < NUM_FINGERS; i++) {                        // get locations for up to to 5 touches
                TouchX[i] = (int) event.getX(i);
                TouchY[i] = (int) event.getY(i);
                inputValueX = (int) (TouchX[0] - StartX);
                inputValueY = (int) (TouchY[0] - StartY);
                if(pointerCount < 3){
                    input_min[pointerCount] = TouchX[0];
                    input_max[pointerCount] = TouchY[0];
                }
            }
        } else if (actionCode == MotionEvent.ACTION_UP && pointerCount > 0) {        // update the distance
            inputValueX = (int) (TouchX[0] - StartX);
            inputValueY = (int) (TouchY[0] - StartY);
            pointerCount = 0;
        }
        return true;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Imgproc.resize(inputFrame.rgba(),smallRgba,zero_size,1/down_samp_scale,1/down_samp_scale,Imgproc.INTER_NEAREST);
        thresholder.read_RGB_image(smallRgba);
        viewMode = 1;
        int tube_turn_value = 0;
        int cone_turn_value = 0;
        switch (viewMode) {
            case 0:
                break;
            case 1:
                tube_turn_value = tube_turn_calc.calculate_turn(thresholder.get_thresh_image(tube_thresh), smallRgba);
                cone_turn_value = cone_turn_calc.calculate_turn(thresholder.get_thresh_image(cone_thresh), smallRgba);
                if(tube_turn_value * cone_turn_value < 0){
                    turn_value = tube_turn_value;
                }
                else{
                    turn_value = (Math.abs(tube_turn_value) > Math.abs(cone_turn_value))?tube_turn_value:cone_turn_value;
                }
                left_blocked = tube_turn_calc.num_free_left_cols < 3 || cone_turn_calc.num_free_left_cols < 3;
                right_blocked = tube_turn_calc.num_free_right_cols < 3 || cone_turn_calc.num_free_right_cols < 3;
                if(left_blocked && right_blocked) {
                    if (tube_turn_calc.num_free_left_cols > tube_turn_calc.num_free_right_cols) {
                        backup_left = true;
                    } else {
                        backup_right = true;
                    }
                }else if(cone_turn_calc.front_blocked){
                    if(cone_turn_calc.prefered_direction > 0){
                        backup_right = true;
                    }
                    else{
                        backup_left = true;
                    }
                }
                else if(tube_turn_calc.front_blocked){
                    if(tube_turn_calc.prefered_direction > 0){
                        backup_right = true;
                    }
                    else{
                        backup_left = true;
                    }
                }


        break;
            case 2:
                turn_value = cone_turn_calc.calculate_turn(thresholder.get_thresh_image(cone_thresh), smallRgba);
                break;
            case 3:
                Imgproc.cvtColor(thresholder.get_thresh_image(cone_thresh), smallRgba, Imgproc.COLOR_GRAY2RGB);
                break;
        }

        Imgproc.resize(smallRgba,mDisplay,zero_size,down_samp_scale,down_samp_scale,Imgproc.INTER_NEAREST);
        Core.putText(mDisplay, "Right blocked: " + right_blocked, new Point(3, 75), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 0, 0, 255), 2);
        Core.putText(mDisplay, "Left blocked: " + left_blocked, new Point(3, 100), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 0, 0, 255), 2);
        Core.putText(mDisplay, "Turn Value: " + turn_value, new Point(3, 50), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 0, 0, 255), 2);
//        Core.putText(mDisplay, "Freq: " + freq, new Point(3, 55), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 0, 0, 255), 2);
        return mDisplay;
    }

    private void print_fps(){
        long cur_sec = System.currentTimeMillis();
        long diff = cur_sec - prev_sec;
        if(diff==0){
            diff = 1;
        }
        long fps = 1000/diff;
        prev_sec = cur_sec;
        Core.putText(mDisplay, "FPS: " + fps, new Point(3, 25), Core.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(255, 0, 0, 255), 2);
    }

    private void PID_calc(int speed){

    }

    /**
     * This is the thread on which all the IOIO activity happens. It will be run
     * every time the application is resumed and aborted when it is paused. The
     * method setup() will be called right after a connection with the IOIO has
     * been established (which might happen several times!). Then, loop() will
     * be called repetitively until the IOIO gets disconnected.
     */
    class Looper extends BaseIOIOLooper {
        private DigitalOutput led_;
        private PwmOutput turnOutput_;		// pwm output for turn motor
        private PwmOutput pwrOutput_;		// pwm output for drive motor
        private PulseInput encoderInput_;   // pulse input to measure speed

        @Override
        protected void setup() throws ConnectionLostException {
            showVersions(ioio_, "IOIO connected!");
            led_ = ioio_.openDigitalOutput(0, true);
            turnOutput_ = ioio_.openPwmOutput(12, 100);     // Hard Left: 2000, Straight: 1400, Hard Right: 1000
            pwrOutput_ = ioio_.openPwmOutput(14, 100);      // Fast Forward: 2500, Stop: 1540, Fast Reverse: 500
            encoderInput_ = ioio_.openPulseInput(3, PulseInput.PulseMode.FREQ);
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            if(backup_left || backup_right){
                backup();
            }
            else {
                desired_speed_freq = 1900 - Math.abs(turn_value*100);
                pid_direction = 1;
            }
            pid_results = get_PID_speed();
            pwrOutput_.setPulseWidth(POWER_OFF + pid_direction*pid_results + POWER_MAX / 2);        // offset by 500
            turnOutput_.setPulseWidth(STEER_OFF + turn_value * 50 + STEER_MAX / 2);       // Offset by 1000
            Thread.sleep(100);
        }
        private void backup()throws ConnectionLostException, InterruptedException{
            switch (backup_state){
                case OFF:
                    desired_speed_freq = 0;
                    pid_direction = 1;
                    backup_count = 0;
                    backup_state = Backup_State.FIRST_STOP;
                    break;
                case FIRST_STOP:
                    backup_count++;
                    desired_speed_freq = 0;
                    pid_direction = 1;
                    if(backup_count >= BACKUP_DURATION){
                        backup_count = 0;
                        backup_state = Backup_State.FIRST_TURN;
                    }
                case FIRST_TURN:
                    desired_speed_freq = BACKUP_FREQ;
                    pid_direction = -1;
                    if(backup_left){
                        turn_value = BACKUP_TURN_VALUE;
                    }else{
                        turn_value = -BACKUP_TURN_VALUE;
                    }
                    backup_count++;
                    if(backup_count >= BACKUP_DURATION){
                        backup_count = 0;
                        backup_state = Backup_State.STRIGHT;
                    }
                    break;
                case STRIGHT:
                    desired_speed_freq = BACKUP_FREQ;
                    pid_direction = -1;
                    turn_value = 0;
                    backup_count++;
                    if(backup_count >= BACKUP_DURATION){
                        backup_count = 0;
                        backup_state = Backup_State.SECOND_TURN;
                    }
                    break;
                case SECOND_TURN:
                    desired_speed_freq = BACKUP_FREQ;
                    pid_direction = -1;
                    if(backup_left){
                        turn_value = -BACKUP_TURN_VALUE;
                    }else{
                        turn_value = BACKUP_TURN_VALUE;
                    }
                    backup_count++;
                    if(backup_count >= BACKUP_DURATION){
                        backup_count = 0;
                        backup_state = Backup_State.SECOND_STOP;
                    }
                    break;
                case SECOND_STOP:
                    backup_count++;
                    desired_speed_freq = 0;
                    pid_direction = 1;
                    if(backup_count >= BACKUP_DURATION){
                        backup_count = 0;
                        backup_state = Backup_State.OFF;
                        backup_left = false;
                        backup_right = false;
                    }
                default:
                    break;
            }
        }
        private int get_PID_speed()throws ConnectionLostException, InterruptedException{
            freq = encoderInput_.getFrequency();
            double error = desired_speed_freq - freq;
            integral += error;
            int ret = (int)(error*K_D + K_I * integral);
            if(ret < 0){
                ret = 0;
            }
            if((K_I * integral) > IOIO_POWER_MAX){
                integral = 0;
            }
            if(ret > IOIO_POWER_MAX){
                ret = IOIO_POWER_MAX;
            }
            return ret;
        }

        @Override
        public void disconnected() {
            toast("IOIO disconnected");
        }

        @Override
        public void incompatible() {
            showVersions(ioio_, "Incompatible firmware version!");
        }
    }

    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    private void showVersions(IOIO ioio, String title) {
        toast(String.format("%s\n" +
                        "IOIOLib: %s\n" +
                        "Application firmware: %s\n" +
                        "Bootloader firmware: %s\n" +
                        "Hardware: %s",
                title,
                ioio.getImplVersion(IOIO.VersionType.IOIOLIB_VER),
                ioio.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER),
                ioio.getImplVersion(IOIO.VersionType.BOOTLOADER_VER),
                ioio.getImplVersion(IOIO.VersionType.HARDWARE_VER)));
    }

    private void toast(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

}
