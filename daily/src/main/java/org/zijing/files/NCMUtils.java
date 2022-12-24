package org.zijing.files;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.io.Files;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

public class NCMUtils {

  public static List<String> readFromStream(InputStream inputStream) throws IOException {
    List<String> lines = Lists.newArrayList();
    try (var br = new BufferedReader(new InputStreamReader(inputStream))) {
      String s;
      while ((s = br.readLine()) != null) {
        lines.add(s);
      }
    }
    return lines;
  }

  public static ProcessExecResult execNCMDump(String command) throws IOException, InterruptedException {
    var process = Runtime.getRuntime().exec(command);
    var exitCode = process.waitFor();
    try (var stdout = process.getInputStream(); var stderr = process.getErrorStream()) {
      return new ProcessExecResult(exitCode, readFromStream(stdout), readFromStream(stderr));
    }
  }

  private static boolean arrayContains(Object target, List list) {
    if (CollectionUtils.isEmpty(list)) {
      return false;
    }
    for (Object o : list) {
      if (Objects.equals(target, o)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsInSuffixes(File file, String... suffixes) {
    return arrayContains(Files.getFileExtension(file.getAbsolutePath()), List.of(suffixes));
  }

  public static List<File> listAllFiles(File ncmPath, String... suffixes) {
    if (ncmPath == null || !ncmPath.isDirectory()) {
      return Lists.newArrayList();
    }
    List<File> allValidateFiles = Lists.newArrayList();
    Queue<File> queue = Queues.newLinkedBlockingQueue();
    queue.add(ncmPath);

    while (!queue.isEmpty()) {
      var workingFile = queue.poll();
      if (workingFile == null) {
        continue;
      }
      var files = workingFile.listFiles();
      if (ArrayUtils.isEmpty(files)) {
        continue;
      }
      for (var file : files) {
        if (file.isDirectory()) {
          queue.add(file);
        } else if (file.isFile() && containsInSuffixes(file, suffixes)) {
          allValidateFiles.add(file);
        }
      }
    }
    return allValidateFiles;
  }

  private static void dumpAllNCMFiles(String ncmRoot) throws IOException, InterruptedException {
    var ncmDumpCommandPattern = "d:/ncmdump/main.exe \"%s\"";

    var allFiles = listAllFiles(new File(ncmRoot), "ncm");
    for (var f : allFiles) {
      var command = String.format(ncmDumpCommandPattern, f.getAbsolutePath());
      var execResult = execNCMDump(command);
      if (execResult.execCode == 0) {
        System.out.println(String.format("Successfully dumped %s", f.getAbsolutePath()));
      } else {
        System.out.println(String.format("Failed dumping(%s).", f.getAbsolutePath()));
        if (CollectionUtils.isNotEmpty(execResult.stderr)) {
          execResult.stderr.stream().map(s -> "\t" + s).forEach(System.out::println);
        }
      }
    }
  }


  private static void copyDumpedFiles(String ncmRoot, String descRoot) throws IOException {
    var allFiles = listAllFiles(new File(ncmRoot), "flac", "mp3");
    if (CollectionUtils.isEmpty(allFiles)) {
      return;
    }
    for (var f : allFiles) {
      var dsc = new File(descRoot, f.getName());
      System.out.printf("Copying...%s%n", f.getAbsolutePath());
      Files.copy(f, dsc);
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    var ncmRoot = "D:/neteaseCloudMusic";
    var descRoot = "d:/music";
    copyDumpedFiles(ncmRoot, descRoot);
  }
}
