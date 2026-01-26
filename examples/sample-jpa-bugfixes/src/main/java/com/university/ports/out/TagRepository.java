package com.university.ports.out;

import com.university.domain.course.Tag;
import com.university.domain.course.TagId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for Tag aggregate.
 *
 * <p>Generated adapter will demonstrate:
 * <ul>
 *   <li>BUG-003: Bidirectional @ManyToMany with mappedBy</li>
 * </ul>
 */
public interface TagRepository {

    Tag save(Tag tag);

    Optional<Tag> findById(TagId id);

    List<Tag> findAll();

    void delete(Tag tag);

    Optional<Tag> findByName(String name);
}
