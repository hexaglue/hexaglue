package ddd;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>
 * A Domain Service, i.e. a feature that belongs to the domain and the ubiquitous
 * language.
 * </p>
 *
 * @see <a href=
 * "https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf">Domain-Driven Design Reference</a>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainService {
}
