package com.linksentinel.web;

import java.util.Map;

public record ApiError(String error, String message, Map<String, String> fields) {
}
