package uy.com.pepeganga.userservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uy.com.pepeganga.business.common.entities.Profile;
import uy.com.pepeganga.business.common.entities.User;
import uy.com.pepeganga.business.common.utils.conversions.Utils;
import uy.com.pepeganga.userservice.repository.ProfileRepository;
import uy.com.pepeganga.userservice.repository.UserRepository;

import java.util.List;
import java.util.Optional;


@Service
public class UserService implements IUserService {

    private final UserRepository userRepository;

    private final ProfileRepository profileRepository;


    public UserService(UserRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }


    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Profile saveUserProfile(Profile profile) {
        if (!Utils.isNumeric(profile.getRut())) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, String.format("Rut is not a number %s", profile.getRut()));
        }

        if (userRepository.existsByEmail(profile.getUser().getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format("User with email: %s exist", profile.getUser().getEmail()));
        }
        User userSaved = userRepository.save(profile.getUser());
        profile.setUser(userSaved);
        return profileRepository.save(profile);
    }

    @Override
    public Profile updateUserProfile(Profile profile, Integer profileId, Integer userId) {

        if (!Utils.isNumeric(profile.getRut())) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, String.format("Rut is not a number %s", profile.getRut()));
        }
        Optional<Profile> profileToUpdatedDb = profileRepository.findById(profileId);
        Optional<User> userToUpdatedDb = userRepository.findById(userId);
        if (profileToUpdatedDb.isPresent() && userToUpdatedDb.isPresent()) {
            User userToUpdate = userToUpdatedDb.get();
            userToUpdate.setEmail(profile.getUser().getEmail());
            userToUpdate.setPassword(profile.getUser().getPassword());
            userToUpdate.setRoles(profile.getUser().getRoles());
            userToUpdate.setMarketplaces(profile.getUser().getMarketplaces());
            userToUpdate.setEnabled(profile.getUser().getEnabled());
            User userUpdated = userRepository.save(userToUpdate);
            profileToUpdatedDb.get().setFirstName(profile.getFirstName());
            profileToUpdatedDb.get().setLastName(profile.getLastName());
            profileToUpdatedDb.get().setBusinessName(profile.getBusinessName());
            profileToUpdatedDb.get().setImage(profile.getImage());
            profileToUpdatedDb.get().setRut(profile.getRut());
            profileToUpdatedDb.get().setStore(profile.getStore());
            profileToUpdatedDb.get().setAddress(profile.getAddress());
            profileToUpdatedDb.get().setUser(userUpdated);
            return profileRepository.save(profileToUpdatedDb.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User not updated with id %s", profile.getUser().getId()));
        }

    }


    @Override
    @Transactional
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User not deleted with id %s, it not exist", id));
        }
        profileRepository.deleteProfileByUserId(id);
        userRepository.deleteById(id);
    }

    @Override
    public User enableOrDisable(Integer id, boolean enableOrDisable) {
        Optional<User> user = userRepository.findById(id);

        if (user.isPresent()) {
            User userToUpdate = user.get();
            userToUpdate.setEnabled(enableOrDisable);
            return userRepository.save(userToUpdate);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("User not enabled or disabled with id %s, it not exist", id));
        }
    }


}
