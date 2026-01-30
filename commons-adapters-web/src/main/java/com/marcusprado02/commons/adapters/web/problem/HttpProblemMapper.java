package com.marcusprado02.commons.adapters.web.problem;

import com.marcusprado02.commons.kernel.errors.Problem;

public interface HttpProblemMapper {

  HttpProblemResponse map(Problem problem);
}
