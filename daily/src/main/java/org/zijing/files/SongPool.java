package org.zijing.files;

import com.google.common.collect.*;
import com.google.common.io.Files;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static com.google.common.collect.ImmutableSet.builder;

public class SongPool {

    public static final String NCM_MUSIC_PATH = "C:/CloudMusic";

    private final String musicPollPath;
    private Map<String, Song> songMap;

    public SongPool(String musicPollPath) {
        this.musicPollPath = musicPollPath;
        this.songMap = Maps.newHashMapWithExpectedSize(1000);
    }

    public SongPool collect() {
        var path = new File(musicPollPath);
        if (!path.exists()) {
            return this;
        }
        songMap = new HashMap<>();
        File[] content = path.listFiles();
        LinkedBlockingQueue<File> q = Queues.newLinkedBlockingQueue();
        q.addAll(Lists.newArrayList(Objects.requireNonNull(content)));
        while (!q.isEmpty()) {
            var file = q.poll();
            if (file.isFile()) {
                if (NCMUtils.containsInSuffixes(file, "ncm", "mp3", "flac")) {
                    var song = Song.createFromFile(file);
                    var existingSong = findSong(song.name());
                    if (existingSong == null || !Objects.equals(existingSong.ext(), "flac")) {
                        songMap.put(song.name(), song);
                    } else {
                        System.out.printf("已经存在更好格式的歌曲文件 - %s VS %s%n", existingSong.absolutePath(), song.absolutePath());
                    }
                }
            } else if (file.isDirectory()) {
                content = file.listFiles();
                if (ArrayUtils.isEmpty(content)) {
                    continue;
                }
                Arrays.stream(Objects.requireNonNull(content)).forEach(q::offer);
            }
        }
        return this;
    }

    public int size() {
        return CollectionUtils.size(songMap);
    }

    public Collection<Song> getAllSongs() {
        return songMap == null ? Collections.emptySet() : songMap.values();
    }

    public Song findSong(String songName) {
        return songMap.get(StringUtils.trimToNull(songName));
    }

    public static void main(String[] args) {
        SongPool pool = new SongPool("C:/CloudMusic").collect();
        System.out.println(pool.size());
    }

    public String getMusicPollPath() {
        return musicPollPath;
    }
}
