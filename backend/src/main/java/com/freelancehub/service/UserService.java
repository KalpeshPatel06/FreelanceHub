package com.freelancehub.service;

import com.freelancehub.dto.response.UserResponse;
import com.freelancehub.entity.User;
import com.freelancehub.exception.BadRequestException;
import com.freelancehub.repository.BidRepository;
import com.freelancehub.repository.ProjectRepository;
import com.freelancehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final BidRepository bidRepository;
    private final S3Client s3Client;

    @Value("${app.aws.s3.bucket}")
    private String bucketName;

    @Value("${app.aws.region}")
    private String region;

    /**
     * Get the currently logged-in user's profile with stats.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserResponse response = UserResponse.from(user);

        // Add stats depending on role
        if (user.getRole() == User.Role.CLIENT) {
            response.setProjectCount(projectRepository.countByClient(user));
        } else {
            response.setBidCount(bidRepository.findByFreelancerOrderByCreatedAtDesc(user).size());
        }

        return response;
    }

    /**
     * Upload a profile image to AWS S3 and save the URL in the database.
     *
     * How S3 upload works:
     *   1. Receive the image file from the HTTP request
     *   2. Generate a unique filename using UUID
     *   3. Upload the file bytes to S3 using AWS SDK
     *   4. Build the public URL (https://bucket.s3.region.amazonaws.com/key)
     *   5. Save the URL on the User entity
     *
     * Note: For this to work:
     *   - Your S3 bucket must have public read access (or use pre-signed URLs)
     *   - AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY env vars must be set
     */
    @Transactional
    public UserResponse uploadProfileImage(MultipartFile file) throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }

        // Generate a unique S3 key (file path inside the bucket)
        String extension = getExtension(file.getOriginalFilename());
        String s3Key = "profiles/" + UUID.randomUUID() + extension;

        // Upload to S3
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest,
                RequestBody.fromBytes(file.getBytes()));

        // Build the public URL
        String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, s3Key);

        user.setProfileImageUrl(imageUrl);
        userRepository.save(user);

        log.info("Profile image uploaded for user {}: {}", email, imageUrl);
        return UserResponse.from(user);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
