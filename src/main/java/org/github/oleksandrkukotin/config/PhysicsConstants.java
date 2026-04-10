package org.github.oleksandrkukotin.config;

public enum PhysicsConstants {
    ;

    /** Mass parameter μ = m_Jupiter / (m_Sun + m_Jupiter) for the Sun–Jupiter system. */
    public static final double MU = 9.5368e-4;

    /** Normalized Sun mass: 1 − μ. */
    public static final double ONE_MINUS_MU = 1.0 - MU;
}