package io.github.diegoquirino.veredito.web;

import jakarta.servlet.http.HttpServletRequest;

/** Whether a request carries a valid administrator session; spec 004 provides the real check. */
public interface AdministratorSessionCheck {

    boolean isAdministrator(HttpServletRequest request);
}
