package com.ble.sensor.tempreature;

//import android.os.Environment;
//import android.util.Log;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.text.SimpleDateFormat;
//import java.util.Calendar;

public class Util {

    public static final String TAG = "Util";
//    private static BufferedWriter logWriter = null;
//    private static String logFilePath = null;
//    private static String logFileName = null;

    /** Create a file Uri for saving an image or video */

//    public static void initLogWriter(){
//        logWriterClose();
//        setLogFile();
//        try {
//            logWriter = new BufferedWriter(new FileWriter(logFilePath,true));
//        }catch (Exception e){
//            logWriter = null;
//            e.printStackTrace();
//        }
//    }

//    public static String getLogFileName(){
//        if( logFileName == null) return "";
//        return logFileName;
//    }

//    private static void setLogFile(){
//        logFileName = "log_TempLogger.txt";
//        logFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator+ "PVLogger"+File.separator + logFileName;
//    }

//    public static void logWriterClose(){
//        if (logWriter != null){
//            try {
//                logWriter.close();
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//    }

//    public static void  printLog(String text)
//    {
//        if (logWriter == null) return;
//        try{
//            Calendar calendar = Calendar.getInstance();
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE,hh-mm-ss a");
//            String time = simpleDateFormat.format(calendar.getTime());
//            logWriter.append(time);
//            logWriter.newLine();
//            logWriter.append(text);
//            logWriter.newLine();
//            String log = time+" :: "+text;
//            Log.e(TAG,log);
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//    }

    public static float getTemperature(byte[] bytes){
        try {
            int temp1 = bytes[19] & 0xFF;
            int temp2 = bytes[20] & 0xFF;
            short temp = (short) (temp2<<8 | temp1);
            float temperature = (float) (-45.0 + 175.0 * (double)temp/65535.0);
            return temperature;
        }catch (Exception e){
            e.printStackTrace();
        }
        return (float) 0.0;

    }
//    public static float Half(byte[] data, int offset){
//        return getHalf(new byte[]{data[offset],data[offset+1]});
//    }
//
//    public static float getHalf(byte[] buffer){
//        byte first = buffer[1];
//        byte second = buffer[0];
//        byte sign = (byte)(first>>7);
//        byte stored = (byte)((first & 0x7C) >>2);
//        byte implicit = (byte)((stored==0)?0:1);
//        short significand = (short)(((short)(first & (short)0x0003)<<8) | (short)second);
//        float result = (float)Math.pow(-1,sign) * (float)Math.pow(2,(stored - 15)) * (implicit + (float)(significand/1024f));
//        return result;
//    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

//    public static String bytesToHex(byte[] bytes) {
//        char[] hexChars = new char[bytes.length * 2];
//        for (int j = 0; j < bytes.length; j++) {
//            int v = bytes[j] & 0xFF;
//            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
//            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
//        }
//        return new String(hexChars);
//    }
}
