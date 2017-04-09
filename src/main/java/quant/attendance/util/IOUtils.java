package quant.attendance.util;

import java.io.*;

public class IOUtils {

    public static String stream2String(final InputStream is) throws IOException {
        final StringBuilder sb = new StringBuilder();
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } finally {
            closeStream(is);
        }
        return sb.toString();
    }

    /**
     * 关闭IO流对象
     *
     * @param streams
     */
    public static void closeStream(Closeable... streams) {
        if (null != streams) {
            try {
                for (int i = 0; i < streams.length; i++) {
                    if (null != streams[i]) {
                        streams[i].close();
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * 获取文件的内容，以字符串的形式返回， 注：暂没有考虑编码问题
     *
     * @return 文件内容，如果没有文件，返回空
     */
    public static String getFileContent(File file) {
        StringBuilder builder = new StringBuilder();
        if (file.exists()) {
            BufferedReader bufferedReader = null;
            try {
                FileReader reader = new FileReader(file);
                bufferedReader = new BufferedReader(reader);
                String lineText;
                while ((lineText = bufferedReader.readLine()) != null) {
                    builder.append(lineText);
                }
            } catch (Exception e) {
            } finally {
                closeStream(bufferedReader);
            }
        }
        return builder.toString();
    }
}
