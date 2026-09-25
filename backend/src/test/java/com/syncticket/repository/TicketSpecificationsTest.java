package com.syncticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketSpecificationsTest {

    @ParameterizedTest
    @CsvSource({
        "plain,plain",
        "100%,100\\%",
        "a_b,a\\_b",
        "back\\slash,back\\\\slash",
        "mix_%\\,mix\\_\\%\\\\"
    })
    void escapeLike(String input, String expected) {
        assertThat(TicketSpecifications.escapeLike(input)).isEqualTo(expected);
    }
}
