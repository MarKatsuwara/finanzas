package com.markmadlang.finanzas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HolaController {

	@GetMapping("/hola")
	public String hola() {
		return "¡Mi app de finanzas funciona!";
	}
}
