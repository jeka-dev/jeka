package org.jake.depmanagement;

import org.junit.Test;

public class Depêndenciestest {

	@Test
	public void test() {
		Dependencies.builder()
		.on("hibernate:hjmlm:1212.0")
		.on("spring:spring:6.3")
		.on(Dependencies.builder()
				.on("454545:5445:54545")
				.on("lkll:llljk:poo").build())
				.on("klklkl:lklk:mlml")
				.on("hhhhh:ll:ppp")
				.on(JakeModuleId.of("lmlmlm:mùmùmù"), JakeVersionRange.of("5454"))
				.build();
	}

}
