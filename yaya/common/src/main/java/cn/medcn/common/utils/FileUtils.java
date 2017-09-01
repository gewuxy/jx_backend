package cn.medcn.common.utils;

import cn.medcn.common.supports.MediaInfo;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;
import org.springframework.util.StringUtils;
import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by lixuan on 2017/1/5.
 */
public class FileUtils {

    public static String readFromInputStream(InputStream inputStream){
        InputStreamReader isr = null;
        BufferedReader br = null;
        try{
            isr = new InputStreamReader(inputStream);
            br = new BufferedReader(isr);
            String str ;
            StringBuilder sb = new StringBuilder();
            while((str = br.readLine()) != null){
                sb.append(str);
            }
            return sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                isr.close();
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static String fileSize2String(Long fileSize){
        int k = 1024;
        int m = k*1024;
        if(fileSize == null){
            fileSize = 0L;
        }
        if(fileSize > m){//大于1M
            return String.format("%.1f",fileSize*0.1*10/m)+"MB";
        }else if(fileSize > k){
            return String.format("%.1f",fileSize*0.1*10/k)+"KB";
        }else{
            return fileSize+"B";
        }
    }

    /**
     * 删除指定文件下的所有文件
     * @param dir
     */
    public static void deleteSubFiles(File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if(file.isFile()){
                file.delete();
            }else{
                deleteSubFiles(file);
            }
        }
    }

    /**
     * 根据路径删除指定问价
     * @param filePath
     */
    public static void deleteTargetFile(String filePath){
        File file = new File(filePath);
        if(file.exists()){
            file.delete();
        }
    }

    /**
     * 获取文件夹下的子文件数目
     * @param dir
     * @return
     */
    public static int countFiles(File dir){
        File[] files = dir.listFiles();
        return files == null ? 0: files.length;
    }

    /**
     * 解析音频媒体信息
     * @param filePath
     * @return
     */
    public static MediaInfo parseAudioMediaInfo(String filePath){
        if(StringUtils.isEmpty(filePath)){
            return null;
        }
        if(filePath.toLowerCase().endsWith(".mp3")){
            return parseMp3(filePath);
        }
        return parseOtherAudio(filePath);
    }


    public static MediaInfo parseMp3(String mp3){
        if(StringUtils.isEmpty(mp3)){
            return null;
        }
        FileInputStream fis= null;
        try {
            fis = new FileInputStream(mp3);
            int b=fis.available();
            Bitstream bitStream=new Bitstream(fis);
            Header header = bitStream.readFrame();
            int bitrate = header.bitrate();
            int time = (int) header.total_ms(b);
            return new MediaInfo(time/1000, bitrate);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        } catch (BitstreamException e){
            e.printStackTrace();
        } finally {
            if(fis!=null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public static MediaInfo parseOtherAudio(String audioFilePath){
        if(StringUtils.isEmpty(audioFilePath)){
            return null;
        }
        File file = new File(audioFilePath);
        if(!file.exists()){
            return null;
        }
        AudioInputStream audioInputStream = null;
        Clip clip = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(file);
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            int bitrate = Math.round(audioInputStream.getFormat().getSampleRate());
            int duration = (int)clip.getMicrosecondLength()/1000/1000;
            return new MediaInfo(duration, bitrate);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e){
            e.printStackTrace();
        } catch (LineUnavailableException e){
            e.printStackTrace();
        } finally {
            if(audioInputStream != null){
                try {
                    audioInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(clip != null){
                clip.close();
            }
        }
        return null;
    }


    public static void downloadNetWorkFile(String urlString, String saveDir, String fileName){
        File dir = new File(saveDir);
        if (!dir.exists()){
            dir.mkdirs();
        }
        String saveFileName = saveDir+File.separator+fileName;
        InputStream is = null;
        OutputStream os = null;
        try{
            URL url = new URL(urlString);
            // 打开连接
            URLConnection conn = url.openConnection();
            is = conn.getInputStream();
            byte[] bytes = new byte[1024];
            int len;
            // 输出的文件流
            os = new FileOutputStream(saveFileName);
            while ((len = is.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
            os.flush();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (os != null){
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }





    public static void main(String[] args) {
        String filePath = "D:/lixuan/test/2651.mp3";
        MediaInfo mediaInfo = parseAudioMediaInfo(filePath);
        if(mediaInfo != null){
            System.out.println("duration = "+mediaInfo.getDuration()+" - bitrate = "+mediaInfo.getBitRate());
        }
    }
}
