package uy.com.pepeganga.userservice.service;

import uy.com.pepeganga.userservice.entities.Profile;

import java.util.List;

public interface IProfileService {

    List<Profile> getProfiles();
}