package org.zijing.files;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Objects;

public record Song(String name, String ext, String absolutePath) implements Comparable<Song> {

    public static Song createFromFile(File songFile) {
        var absolutePath = songFile.getAbsolutePath();
        var songName = Files.getNameWithoutExtension(absolutePath);
        var ext = Files.getFileExtension(absolutePath);
        return new Song(songName, ext, absolutePath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return Objects.equals(absolutePath, song.absolutePath);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(absolutePath);
    }

    @Override
    public String toString() {
        return absolutePath;
    }


    @Override
    public int compareTo(Song o) {
        return StringUtils.compare(absolutePath, o.absolutePath);
    }
}
