package com.example.business.service;

import java.net.URI;

public interface RemoteFileFetcher {
    RemoteFilePayload fetch(URI source);

    record RemoteFilePayload(URI finalUri, String contentType, String filename, byte[] bytes) {
    }
}
