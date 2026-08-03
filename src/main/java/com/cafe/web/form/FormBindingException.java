package com.cafe.web.form;

/** Lỗi cú pháp request; không chứa quy tắc nghiệp vụ. */
public final class FormBindingException extends RuntimeException {
    public FormBindingException(String message) {
        super(message);
    }
}
