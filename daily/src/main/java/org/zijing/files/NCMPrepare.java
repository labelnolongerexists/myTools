package org.zijing.files;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;

public class NCMPrepare {

    private static final String INFO_FILE_NAME = "info";

    private static final String PLAYLIST_WORKSPACE = "C:/music/playlists";
    private static final String MUSIC_POOL_DIR = "C:/music/raw";
    private static final String PLAYLIST_TANGJIALING = "Once upon a time in TJL";

    private static final DecimalFormat DF = new DecimalFormat("000");

    private int updateCurrentNumberInPlaylist(File infoFile, int currentNumberInPlaylist) throws FileNotFoundException {
        try (var pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(infoFile)))) {
            pw.write(DF.format(currentNumberInPlaylist));
            pw.write("\n");
        }
        return currentNumberInPlaylist;
    }

    private int loadCreateInfoFile(File playListPathFile) throws IOException, IllegalAccessException {
        if (playListPathFile == null || !playListPathFile.exists()) {
            throw new FileNotFoundException();
        }
        if (!playListPathFile.getAbsolutePath().contains(PLAYLIST_WORKSPACE)) {
            throw new IllegalAccessException(String.format("invalid workspace - %s", playListPathFile.getAbsolutePath()));
        }
        var infoFile = new File(playListPathFile.getAbsolutePath() + File.separator + INFO_FILE_NAME);
        if (infoFile.createNewFile()) {
            System.out.printf("Created new info file in %s%n", playListPathFile.getAbsolutePath());
            return updateCurrentNumberInPlaylist(infoFile, 0);
        }
        int currentNumber = 0;
        try (var br = new BufferedReader(new InputStreamReader(new FileInputStream(infoFile)))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = StringUtils.trimToEmpty(line);
                if (StringUtils.isNumeric(line)) {
                    currentNumber = Integer.parseInt(line);
                }
            }
        }
        return currentNumber;
    }

    private static File findSrcMusicFile(String musicRoot, String songName) {
        var files = new File(musicRoot);

        for (var musicFile : files.listFiles()) {
            if (!musicFile.getName().contains(songName)) {
                continue;
            }
            var ext = Files.getFileExtension(musicFile.getName());
            if (StringUtils.equalsAnyIgnoreCase(ext, "flac", "mp3", "ncm")) {
                return musicFile;
            }
        }
        return null;
    }

    private static int removeAllExistingMusicFile(File playlistDir) throws IOException {
        var musicFileFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.exists() && pathname.isFile() && StringUtils.equalsAnyIgnoreCase(Files.getFileExtension(pathname.getAbsolutePath()), "ncm", "mp3", "flac");
            }
        };
        int counter = 0;
        for (File musicFile : Objects.requireNonNull(playlistDir.listFiles(musicFileFilter))) {
            if (java.nio.file.Files.deleteIfExists(Paths.get(musicFile.toURI()))) {
                ++counter;
            }
        }
        return counter;
    }

    public static void main(String[] args) throws IOException {
        var playlistDir = new File(PLAYLIST_WORKSPACE + File.separator + PLAYLIST_TANGJIALING);
        if (!playlistDir.exists()) {
            return;
        }
        var playlistInfo = new File(playlistDir.getAbsolutePath() + File.separator + INFO_FILE_NAME);
        if (!playlistInfo.exists()) {
            return;
        }

        if (removeAllExistingMusicFile(playlistDir) > 0) {
            System.out.printf("removed all musics files in %s%n", playlistDir.getAbsolutePath());
        }

        var successCount = 0;
        var failedCount = 0;
        try (var br = new BufferedReader(new InputStreamReader(new FileInputStream(playlistInfo)))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = StringUtils.trimToEmpty(line);
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                var songName = line;
                var srcMusicFile = findSrcMusicFile(MUSIC_POOL_DIR, songName);
                if (srcMusicFile == null || !srcMusicFile.exists()) {
                    ++failedCount;
                    System.out.printf("music file does not exist - %s%n", songName);
                    continue;
                }
                ++successCount;
                var srcMusicExt = Files.getFileExtension(srcMusicFile.getName());
                var number = DF.format(successCount);
                var distMusicFileName = number + "." + line;
                var distMusicFile = new File(playlistDir.getAbsolutePath() + File.separator + distMusicFileName + "." + srcMusicExt);
                System.out.printf("[%s] copy %s to %s%n", number, srcMusicFile, distMusicFile);
                Files.copy(srcMusicFile, distMusicFile);
            }
        }
        System.out.printf("all done, %d succeeded and %d failed%n", successCount, failedCount);
    }
}
