package com.booking.userservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@CrossOrigin("*")
@RestController
@RequestMapping("/users")
public class ImageController {

    @Value("gs://booking-project/")
    Resource gcsFile;

    @GetMapping(
            value = "/image/**",
            produces = MediaType.IMAGE_JPEG_VALUE
    )
    public ResponseEntity<byte[]> getImageWithMediaType(ServerHttpRequest request) throws IOException {
        String path = request.getPath().toString().replace("/users/image/","");

        Resource resource = gcsFile.createRelative(path);

        byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());

        return ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }
}
