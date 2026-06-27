package dev.geco.gmusic.service;

import dev.geco.gmusic.GMusicMain;
import dev.geco.gmusic.model.Song;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;

public class SongService {

    public static final String GNBS_EXTENSION = "gnbs";
    public static final String MIDI_EXTENSION = "midi";
    public static final String MID_EXTENSION = "mid";
    public static final String NBS_EXTENSION = "nbs";
    public static final String WAV_EXTENSION = "wav";
    public static final String GNBS_FOLDER = "gnbs";
    public static final String CONVERT_FOLDER = "convert";

    private final GMusicMain gMusicMain;
    private final TreeMap<String, Song> songs = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public SongService(GMusicMain gMusicMain) {
        this.gMusicMain = gMusicMain;
    }

    public List<Song> getSongs() { return new ArrayList<>(songs.values()); }

    public @Nullable Song getSongById(@NotNull String song) { return songs.get(song.toLowerCase()); }

    public List<Song> filterSongsBySearch(@NotNull List<Song> songs, @NotNull String search) { return songs.stream().filter(song -> song.getTitle().toLowerCase().contains(search.toLowerCase())).toList(); }

    public void loadSongs() {
        unloadSongs();

        convertFolder();

        convertSongs();

        File gnbsDir = new File(gMusicMain.getDataFolder(), GNBS_FOLDER);
        if(!gnbsDir.exists()) return;

        loadSongFolder(gnbsDir);
    }

    private void loadSongFolder(@NotNull File dir) {
        File[] files = dir.listFiles();
        if(files == null) return;
        for(File file : files) {
            if(file.isDirectory()) loadSongFolder(file);
            else loadSongFile(file);
        }
    }

    public boolean loadSongFile(@NotNull File file) {
        int extensionPos = file.getName().lastIndexOf(".");
        if(extensionPos <= 0 || !file.getName().substring(extensionPos + 1).equalsIgnoreCase(GNBS_EXTENSION)) return false;

        try {
            Song song = new Song(file, getGnbsRelativeName(file));
            if(song.getNoteAmount() == 0) {
                gMusicMain.getLogger().warning("Could not load " + GNBS_EXTENSION + " music '" + file.getName().substring(0, extensionPos) + "', no notes found");
                return false;
            }

            songs.put(song.getId().toLowerCase(), song);
            return true;
        } catch(Throwable e) {
            gMusicMain.getLogger().log(Level.WARNING, "Could not load " + GNBS_EXTENSION + " music '" + file.getName().substring(0, extensionPos) + "'", e);
            return false;
        }
    }

    public void unloadSongs() {
        songs.clear();
    }

    private String getGnbsRelativeName(@NotNull File file) {
        File gnbsDir = new File(gMusicMain.getDataFolder(), GNBS_FOLDER);
        String filePath = file.getAbsolutePath();
        String gnbsPath = gnbsDir.getAbsolutePath();
        if(!filePath.startsWith(gnbsPath)) return file.getName();

        String relative = filePath.substring(gnbsPath.length()).replace(File.separatorChar, '/');
        if(relative.startsWith("/")) relative = relative.substring(1);
        return relative;
    }

    private void convertFolder() {
        File gnbsDir = new File(gMusicMain.getDataFolder(), GNBS_FOLDER);
        if(!gnbsDir.exists() && !gnbsDir.mkdir()) return;

        File songsDir = new File(gMusicMain.getDataFolder(), "songs");
        if(songsDir.exists()) {
            try {
                Files.move(songsDir.toPath(), gnbsDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.delete(songsDir.toPath());
            } catch(Throwable ignored) {}
        }

        File convertDir = new File(gMusicMain.getDataFolder(), CONVERT_FOLDER);
        if(!convertDir.exists() && !convertDir.mkdir()) return;

        File midiDir = new File(gMusicMain.getDataFolder(), "midi");
        if(midiDir.exists()) {
            try {
                Files.move(midiDir.toPath(), convertDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch(Throwable ignored) {}
        }

        File nbsDir = new File(gMusicMain.getDataFolder(), "nbs");
        if(nbsDir.exists()) {
            try {
                Files.move(gnbsDir.toPath(), convertDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch(Throwable ignored) {}
        }
    }

    public void convertSongs() {
        File gnbsDir = new File(gMusicMain.getDataFolder(), GNBS_FOLDER);
        if(!gnbsDir.exists() && !gnbsDir.mkdir()) {
            gMusicMain.getLogger().severe("Could not create '" + GNBS_FOLDER + "' directory!");
            return;
        }

        File convertDir = new File(gMusicMain.getDataFolder(), CONVERT_FOLDER);
        if(!convertDir.exists() && !convertDir.mkdir()) {
            gMusicMain.getLogger().severe("Could not create '" + CONVERT_FOLDER + "' directory!");
            return;
        }

        convertSongFolder(convertDir);
    }

    private void convertSongFolder(@NotNull File dir) {
        File[] convertFiles = dir.listFiles();
        if(convertFiles == null) return;

        for(File file : convertFiles) {
            if(file.isDirectory()) convertSongFolder(file);
            else convertSongFile(file);
        }
    }

    public @Nullable File convertSongFile(@NotNull File file) {
        File convertDir = new File(gMusicMain.getDataFolder(), CONVERT_FOLDER);
        File gnbsDir = new File(gMusicMain.getDataFolder(), GNBS_FOLDER);

        String relativeParent = "";
        File parent = file.getParentFile();
        if(parent != null) {
            String parentPath = parent.getAbsolutePath();
            String convertPath = convertDir.getAbsolutePath();
            if(parentPath.startsWith(convertPath)) {
                relativeParent = parentPath.substring(convertPath.length()).replace(File.separatorChar, '/');
                if(!relativeParent.isEmpty() && !relativeParent.endsWith("/")) relativeParent += "/";
                if(relativeParent.startsWith("/")) relativeParent = relativeParent.substring(1);
            }
        }

        File gnbsFile = new File(gnbsDir, relativeParent + file.getName().replaceFirst("[.][^.]+$", "") + "." + GNBS_EXTENSION);
        if(gnbsFile.exists()) return gnbsFile;

        String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        switch(extension.toLowerCase()) {
            case MID_EXTENSION:
            case MIDI_EXTENSION:
                if(gMusicMain.getMidiConverter().convertMidiFile(file, gnbsFile)) return gnbsFile;
                return null;
            case NBS_EXTENSION: {
                if(gMusicMain.getNBSConverter().convertNBSFile(file, gnbsFile)) return gnbsFile;
                return null;
            }
            case WAV_EXTENSION: {
                if(gMusicMain.getWavConverter().convertWavFile(file, gnbsFile)) return gnbsFile;
                return null;
            }
            default:
                gMusicMain.getLogger().warning("Invalid convert extension: " + extension);
        }

        return null;
    }

}