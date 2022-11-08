package au.com.shiftyjelly.pocketcasts.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import timber.log.Timber;

public class FileUtil {

    public static void deleteFileByPath(String path) {
        if (StringUtil.isBlank(path)) return;

        try {
            File file = new File(path);
            if (file.exists()) {
                boolean didDelete = file.delete();
                if (!didDelete) {
                    Timber.e("Could not delete file %s - No exception", path);
                }
            }
        } catch (Exception e){
            Timber.e(e, "Could not delete file %s", path);
        }
    }

    public static void deleteDirectoryContents(String path) {
        try {
            File directory = new File(path);
            if (!directory.isDirectory()) {
                return;
            }

            File[] files = directory.listFiles();
            for (File file : files) {
                if (!file.getName().equalsIgnoreCase(".nomedia")) {
                    file.delete();
                }
            }
        } catch (Exception e) {
            Timber.e("Could not delete directory: " + e.getMessage());
        }
    }

    public static final String readAssetToString(Context context, String filename) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));
            StringBuilder html = new StringBuilder();
            String line;
            String newLine = "\n";
            while((line = reader.readLine()) != null) {
                html.append(line).append(newLine);
            }
            return html.toString();
        }
        catch (IOException e) {
            Timber.e(e);
            return null;
        }
        finally {
            try { if (reader != null) { reader.close(); } } catch(Exception e) {}
        }
    }

    public static final String readFileToString(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fileReader);
        String line;
        StringBuilder output = new StringBuilder((int)file.length());
        String newLine = "\n";
        while((line = reader.readLine()) != null) {
            output.append(line).append(newLine);
        }
        try { fileReader.close(); } catch(Exception e) {}
        try { reader.close(); } catch(Exception e) {}
        return output.toString();
    }

    public static void readFileTo(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);

        //	Transfer bytes from in to out
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        finally {
            in.close();
        }
    }

    public static void copy(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            copyDirectory(src, dst);
        }
        else {
            copyFile(src, dst);
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        dst.getParentFile().mkdirs();
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        //	Transfer bytes from in to out
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        finally {
            in.close();
            out.close();
        }
    }

    // Copies all files under srcDir to dstDir.
    // If dstDir does not exist, it will be created.
    public static void copyDirectory(File srcDir, File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            if (!dstDir.exists()) {
                dstDir.mkdir();
            }

            String[] children = srcDir.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(srcDir, children[i]), new File(dstDir, children[i]));
            }
        }
        else {
            copyFile(srcDir, dstDir);
        }
    }

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName == null || fileName.length() == 0) return null;

        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0) return null; //no dot in file name

        return fileName.substring(dotIndex);
    }

    public static String getFileNameWithoutExtension(File file) {
        if (file == null) {
            return null;
        }

        String name = file.getName();
        if (name == null) {
            return null;
        }

        int dotIndex = name.lastIndexOf(".");
        if (dotIndex == -1) {
            return name;
        }
        return name.substring(0, dotIndex);
    }


    public static Uri createUriWithReadPermissions(@NonNull File file, @NonNull Intent intent, @NonNull Context context) {
        Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", file);
        // give the email clients read access
        List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        return uri;
    }

    public static Uri getUriForFile(@NonNull Context context, @NonNull File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

    public static long folderSize(File directory) {
        if (!directory.isDirectory()) {
            return 0;
        }

        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                length += file.length();
            } else {
                length += folderSize(file);
            }
        }
        return length;
    }
}
