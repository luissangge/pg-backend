package uy.com.pepeganga.userservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uy.com.pepeganga.userservice.entities.Profile;
import uy.com.pepeganga.userservice.repository.ProfileRepository;

import java.util.List;

@Service
public class ProfileService implements IProfileService{

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public List<Profile> getProfiles() {
        return profileRepository.findAll();
    }
}