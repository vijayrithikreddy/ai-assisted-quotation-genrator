package com.vlg.quotation.domain.calculation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AmountInWordsConverterTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "0, 'Zero Only'",
        "5, 'Five Only'",
        "19, 'Nineteen Only'",
        "20, 'Twenty Only'",
        "99, 'Ninety Nine Only'",
        "100, 'One Hundred Only'",
        "101, 'One Hundred One Only'",
        "1000, 'One Thousand Only'",
        "10738, 'Ten Thousand Seven Hundred Thirty Eight Only'",
        "33158, 'Thirty Three Thousand One Hundred Fifty Eight Only'",
        "47200, 'Forty Seven Thousand Two Hundred Only'",
        "100000, 'One Lakh Only'",
        "210000, 'Two Lakh Ten Thousand Only'",
        "10000000, 'One Crore Only'",
        "24780000, 'Two Crore Forty Seven Lakh Eighty Thousand Only'",
        "1000000000, 'One Arab Only'"
    })
    void shouldConvertUsingIndianNumbering(long rupees, String expectedWords) {
        assertThat(AmountInWordsConverter.convert(rupees)).isEqualTo(expectedWords);
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> AmountInWordsConverter.convert(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
