
package mw.loancompliance.user.service;

import mw.loancompliance.user.dto.UpdateUserRequest;
import mw.loancompliance.user.dto.UserResponse;
import mw.loancompliance.user.entity.User;
import mw.loancompliance.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found with ID: " + id));

        return toUserResponse(user);
    }

    @Transactional
public UserResponse updateUser(Long id, UpdateUserRequest request) {
    User user = userRepository.findById(id)
            .orElseThrow(() ->
                    new IllegalArgumentException("User not found with ID: " + id));

    user.setFullName(request.fullName());
    user.setOrganizationId(request.organizationId());

    User updatedUser = userRepository.save(user);

    return toUserResponse(updatedUser);
}
    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getAccountStatus(),
                user.getOrganizationId(),
                user.getEmailVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
