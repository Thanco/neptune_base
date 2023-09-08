package host.thanco.NeptuneBase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


public class ProfileHandler {
    private ArrayList<Profile> profiles;
    private static final String PROFILES_PATH = "json/profiles.json";

    public void loadProfiles() {
        if (!new File(PROFILES_PATH).exists() || !new File("profiles/").isDirectory()) {
            try {
                new File(PROFILES_PATH).createNewFile();
                new File("profiles/").mkdir();
                profiles = new ArrayList<>();
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try (FileReader reader = new FileReader(PROFILES_PATH)) {
            profiles = new Gson().fromJson(reader, new TypeToken<ArrayList<Profile>>() {}.getType());
            for (Profile profile : profiles) {
                File imageFile = new File("profiles/" + profile.getUserName() + ".jpg");
                if (!imageFile.isFile()) {
                    profile.setImageBytes("");
                    continue;
                }
                byte[] imgBytes = Files.readAllBytes(Paths.get(imageFile.toURI()));
                profile.setImageBytes(ImageHandler.compressImageBytes(imgBytes));
            }
        } catch(Exception e) {
            profiles = new ArrayList<>();
            e.printStackTrace();
        }
    }

    public void saveProfiles() {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(PROFILES_PATH))) {
            ArrayList<Profile> temp = new ArrayList<>();
            for (Profile profile : profiles) {
                // compressed string for an empty array is 29 characters long
                if (profile.getCompressedImageBytes().length() > 29) {
                    File imageFile = new File("profiles/" + profile.getUserName() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(imageFile);
                    
                    fos.write(ImageHandler.decompressImageBytes(profile.getCompressedImageBytes()));
                    fos.close();
                }
                temp.add(new Profile(profile.getUserName(), profile.getColor()));
            }
            new GsonBuilder().setPrettyPrinting().create().toJson(temp, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addProfile(Profile newProfile) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).getUserName().equals(newProfile.getUserName())) {
                Profile p = profiles.get(i);
                profiles.remove(p);
            }
        }
        profiles.add(newProfile);
        saveProfiles();
    }

    public void addProfile(String userName, int color) {
        profiles.add(new Profile(userName, color));
        saveProfiles();
    }

    public void removeProfile(String userName) {
        profiles.removeIf((profile) -> profile.getUserName().equals(userName));
        saveProfiles();
    }

    public void setProfiles(ArrayList<Profile> profiles) {
        this.profiles = profiles;
    }

    public ArrayList<Profile> getProfiles() {
        return profiles;
    }

}
