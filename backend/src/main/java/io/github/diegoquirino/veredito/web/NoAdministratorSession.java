package io.github.diegoquirino.veredito.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** There are no sessions before spec 004, so nobody is the administrator. */
@Component
public class NoAdministratorSession implements AdministratorSessionCheck {

    @Override
    public boolean isAdministrator(HttpServletRequest request) {
        return false;
    }
}
