package com.doom.mvc.test;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.doom.mvc.annotation.Handler;
import com.doom.mvc.annotation.Response;

@Handler(path = "/test")
public class TestService {

	@Response(path = "/method0")
	public String method0(String id) {
		return id;
	}

	@Response(path = "/method1")
	public void method1(HttpServletResponse response, HttpServletRequest request) throws IOException {
		response.getOutputStream().write(request.getRemoteAddr().getBytes());
	}
}
