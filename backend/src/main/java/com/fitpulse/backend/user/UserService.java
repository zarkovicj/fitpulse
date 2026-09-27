package com.fitpulse.backend.user;

import com.fitpulse.backend.common.ApiException;
import com.fitpulse.backend.user.dto.UpdateUserRequest;
import com.fitpulse.backend.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(findOrThrow(userId));
    }

    @Transactional
    public UserResponse updateMe(Long userId, UpdateUserRequest request) {
        User user = findOrThrow(userId);

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setBirthDate(request.birthDate());
        user.setHeight(request.height());

        return UserResponse.from(user);
    }

    private User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Korisnik nije pronađen"));
    }
}
