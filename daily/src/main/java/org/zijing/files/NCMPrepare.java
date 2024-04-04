package org.zijing.files;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Set;

public class NCMPrepare {

    private static final String INFO_FILE_NAME = "info";
    private static final String PLAYLIST_WORKSPACE = "C:/music/playlists";
    private static final String PLAYLIST = "四点的唐家岭只属于我";

    private static final DecimalFormat DF = new DecimalFormat("000");

    private static final Set<String> PROCESSED_SONGS = Sets.newHashSet();

//    private int updateCurrentNumberInPlaylist(File infoFile, int currentNumberInPlaylist) throws FileNotFoundException {
//        try (var pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(infoFile)))) {
//            pw.write(DF.format(currentNumberInPlaylist));
//            pw.write("\n");
//        }
//        return currentNumberInPlaylist;
//    }

//    private int loadCreateInfoFile(File playListPathFile) throws IOException, IllegalAccessException {
//        if (playListPathFile == null || !playListPathFile.exists()) {
//            throw new FileNotFoundException();
//        }
//        if (!playListPathFile.getAbsolutePath().contains(PLAYLIST_WORKSPACE)) {
//            throw new IllegalAccessException(String.format("invalid workspace - %s", playListPathFile.getAbsolutePath()));
//        }
//        var infoFile = new File(playListPathFile.getAbsolutePath() + File.separator + INFO_FILE_NAME);
//        if (infoFile.createNewFile()) {
//            System.out.printf("Created new info file in %s%n", playListPathFile.getAbsolutePath());
//            return updateCurrentNumberInPlaylist(infoFile, 0);
//        }
//        int currentNumber = 0;
//        try (var br = new BufferedReader(new InputStreamReader(new FileInputStream(infoFile)))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                line = StringUtils.trimToEmpty(line);
//                if (StringUtils.isNumeric(line)) {
//                    currentNumber = Integer.parseInt(line);
//                }
//            }
//        }
//        return currentNumber;
//    }

//    private static File findSrcMusicFile(String musicRoot, String songName) {
//        var files = new File(musicRoot);
//
//        for (var musicFile : files.listFiles()) {
//            if (!musicFile.getName().contains(songName)) {
//                continue;
//            }
//            var ext = Files.getFileExtension(musicFile.getName());
//            if (StringUtils.equalsAnyIgnoreCase(ext, "flac", "mp3", "ncm")) {
//                return musicFile;
//            }
//        }
//        return null;
//    }

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

    private static String checkDup(String songName) {
        var s = StringUtils.trimToEmpty(songName);
        if (StringUtils.isBlank(s)) {
            return StringUtils.EMPTY;
        }
        if (PROCESSED_SONGS.contains(s)) {
            return s;
        }
        PROCESSED_SONGS.add(s);
        return StringUtils.EMPTY;
    }

    public static void main(String[] args) throws IOException {
        var playlistDir = new File(PLAYLIST_WORKSPACE + File.separator + PLAYLIST);
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

        var ncmSongPool = new SongPool(SongPool.NCM_MUSIC_PATH).collect();
        if (ncmSongPool.size() == 0) {
            System.out.printf("There's no song in %s%n", SongPool.NCM_MUSIC_PATH);
            return;
        }
        System.out.printf("Found %d songs in %s%n", ncmSongPool.size(), ncmSongPool.getMusicPollPath());

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
                if (StringUtils.isNotBlank(checkDup(songName))) {
                    System.out.printf("%s is processed%n", songName);
                    continue;
                }
                var song = ncmSongPool.findSong(songName);
                if (song == null) {
                    ++failedCount;
                    System.out.printf("music file does not exist - %s%n", songName);
                    continue;
                }
                ++successCount;
                var songAbsolutePath = song.absolutePath();
                var srcMusicExt = Files.getFileExtension(songAbsolutePath);
                var number = DF.format(successCount);
                var distMusicFileName = number + "." + line;
                var distMusicFile = new File(playlistDir.getAbsolutePath() + File.separator + distMusicFileName + "." + srcMusicExt);
                System.out.printf("[%s] copy %s to %s%n", number, songAbsolutePath, distMusicFile);
                Files.copy(new File(songAbsolutePath), distMusicFile);
            }
        }
        System.out.printf("all done, %d succeeded and %d failed%n", successCount, failedCount);
    }
}
