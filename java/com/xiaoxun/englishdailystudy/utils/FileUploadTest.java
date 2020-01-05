package com.xiaoxun.englishdailystudy.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class FileUploadTest {
	/**
     * @param url 请求地址
     * @param xxtext 请求json加密串
     * @param fileName 文件路径
     * @param body_data 上传的文件二进制内容
     * @return */
    public static String doPostSubmitBody(String url, String xxtext, String fileName, byte[] body_data) throws Exception{
        final String NEWLINE = "\r\n"; // 换行，或者说是回车
        final String PREFIX = "--"; // 固定的前缀
        final String BOUNDARY = "xunaudio";
        HttpURLConnection httpConn = null;
        BufferedInputStream bis = null;
        DataOutputStream dos = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            URL urlObj = new URL(url);
            
            boolean useHttps = url.startsWith("https");
            if (useHttps) {
            	trustAllHttpsCertificates();
                HostnameVerifier hv = new HostnameVerifier() {
                    public boolean verify(String urlHostName, SSLSession session) {
                        return true;
                    }
                };
                HttpsURLConnection.setDefaultHostnameVerifier(hv);
            }
            
            httpConn = (HttpURLConnection) urlObj.openConnection();
            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setUseCaches(false);
            httpConn.setRequestProperty("Connection", "Keep-Alive");
            httpConn.setRequestProperty("Accept", "*/*");
            httpConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            httpConn.setRequestProperty("Cache-Control", "no-cache");
            httpConn.setRequestProperty("xx-text", xxtext);
            
            httpConn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
            httpConn.connect();

            dos = new DataOutputStream(httpConn.getOutputStream());

            if (body_data != null && body_data.length > 0) {
                dos.writeBytes(PREFIX + BOUNDARY + NEWLINE);// 像请求体中写分割线，就是前缀+分界线+换行
                // 格式是:Content-Disposition: form-data; name="请求参数名"; filename="文件名"
                // 我这里吧请求的参数名写成了audio，是死的，实际应用要根据自己的情况修改
                // 不要忘了换行
                dos.writeBytes("Content-Disposition: form-data; " + "name=\"" + "audio" + "\"" + "; filename=\"" + fileName + "\"" + NEWLINE);
                // 换行，重要！！不要忘了
                dos.writeBytes(NEWLINE);
                dos.write(body_data); // 上传文件的内容
                dos.writeBytes(NEWLINE); // 最后换行
            }
            dos.writeBytes(PREFIX + BOUNDARY + PREFIX + NEWLINE);//最后的分割线，与前面的有点不一样是前缀+分界线+前缀+换行，最后多了一个前缀
            dos.flush();

            byte[] buffer = new byte[8 * 1024];
            int rc = httpConn.getResponseCode();
            Object obj = httpConn.getHeaderFields();
            if (httpConn.getResponseCode() == 200) {
                bis = new BufferedInputStream(httpConn.getInputStream());
                int c = 0;
                while ((c = bis.read(buffer)) != -1) {
                    baos.write(buffer, 0, c);
                    baos.flush();
                }
                return new String(baos.toByteArray(), "utf-8");
            }
        } finally {
            try {
                if (dos != null)
                    dos.close();
                if (bis != null)
                    bis.close();
                if (baos != null)
                    baos.close();
                httpConn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    private static void trustAllHttpsCertificates() throws Exception {
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
    
    static class miTM implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }
    }
    
	public static void main(String[] args) throws Exception{
		String url = "https://cloud.imibaby.net/englishstudy/device/wordscore";
		String fileName = "apple.wav";
		String xxtext = "8zRL21Ioq0V/sAorVw73CtkJapUBpLFbFd5l8tlz8GVp+nzqItH8NAJhzjErpwBrgGaHO0aI4pqUcGSuJwQHvjK31ugFmHaMxaf05TuhMG8JbK/DXHKiGRZ9LDV9P32NBt0bDAmLXolpR5FHyYjEQfN3WWxM2RhpGHWiJPpzVjuOPme6L8tvXcEdDS53ZlxX5GSnN2aRw1i79to277/RKA==F189DC938B5A4A02BC8865F975873985";
		
		FileInputStream inputStream = new FileInputStream("F:/english/YSY-11/juice.wav");
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int bytesRead;
        byte[] buffer_up = new byte[2048];
        while((bytesRead = inputStream.read(buffer_up)) != -1) {
        	outStream.write(buffer_up, 0, bytesRead);
        }
        outStream.flush();
        outStream.close();
        inputStream.close();
        
        byte[] bytes = outStream.toByteArray();
        
        String str = doPostSubmitBody(url, xxtext, fileName, bytes);
        System.out.println(str);
	}
}
