import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by 杜鹏豪 on 2023/8/22.
 */
public class SyncProject extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        String udir = System.getProperty("user.dir");
        Properties configProp = loadConfig(udir);

        String projectName = project == null ? "" : project.getName();

        String sourceDirPath = configProp.getProperty(projectName + "sourceDir");
        String destDirPath = configProp.getProperty(projectName + "destDir");
        String ignoreDirsStr = configProp.getProperty(projectName + "ignoreDirs");
        Set<String> ignoreDirs = new HashSet<String>();
        ignoreDirs.addAll(Arrays.asList(ignoreDirsStr.split(",")));
//        String ignoreFiles = configProp.getProperty(projectName + "ignoreFiles");
        long start = System.currentTimeMillis();
        String message = "";
        try {
            List<String> syncFiles = doSync(sourceDirPath, destDirPath, ignoreDirs);
        } catch (IOException e1) {
            message = e1.getCause().getMessage();
        }
        long end = System.currentTimeMillis();
        Messages.showMessageDialog(project, "项目:" + projectName + sourceDirPath + "=>" + destDirPath + "同步完成,耗时:" + (end - start) + "ms" + "异常:" + message, "同步文件", Messages.getInformationIcon());

    }

    private Properties loadConfig(String udir) {
        //1.读取配置文件
        File configFile = new File(udir, "syncconfig.properties");
        Properties configProp = new Properties();
        FileInputStream inStream = null;
        try {
            inStream = new FileInputStream(configFile);
            configProp.load(inStream);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return configProp;
    }


    private List<String> doSync(String sourceDirPath, String destDirPath, Set<String> ignoreDirs) throws IOException {
        List<String> result = new ArrayList<>();

        //1.同步两个文件夹中的文件
        Files.walkFileTree(Paths.get(sourceDirPath), new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String dirName = dir.toFile().getName();
                if (ignoreDirs.contains(dirName)) {
                    return FileVisitResult.SKIP_SUBTREE;
                } else {
                    String dirPath = dir.toString();
                    String destPath = dirPath.replace(sourceDirPath, destDirPath);
                    File destDir = new File(destPath);
                    if (!destDir.exists())
                        destDir.mkdirs();
                    return FileVisitResult.CONTINUE;
                }
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String sourceFilePath = file.toString();
                String destsourceFilePath = sourceFilePath.replace(sourceDirPath, destDirPath);
                File destFile = new File(destsourceFilePath);
                if (!destFile.exists()) {
                    Files.copy(file, Paths.get(destsourceFilePath), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    File sourceFile = file.toFile();
                    long sourceLen = sourceFile.length();
                    long destLen = destFile.length();

                    long sourceLastModified = sourceFile.lastModified();
                    long destLastModified = destFile.lastModified();

                    if (sourceLen != destLen || sourceLastModified != destLastModified) {
                        Files.copy(file, Paths.get(destsourceFilePath), StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        return result;
    }

}
