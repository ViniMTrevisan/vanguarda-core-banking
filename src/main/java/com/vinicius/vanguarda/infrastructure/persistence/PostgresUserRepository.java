package com.vinicius.vanguarda.infrastructure.persistence;

import com.vinicius.vanguarda.domain.entity.User;
import com.vinicius.vanguarda.domain.repository.UserRepository;
import com.vinicius.vanguarda.infrastructure.persistence.entity.UserEntity;
import com.vinicius.vanguarda.infrastructure.persistence.repository.JpaUserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PostgresUserRepository implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    public PostgresUserRepository(JpaUserRepository jpaUserRepository) {
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public Optional<User> findById(String id) {
        return jpaUserRepository.findById(id).map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(UserEntity::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        return jpaUserRepository.save(entity).toDomain();
    }

    @Override
    public void delete(String id) {
        jpaUserRepository.deleteById(id);
    }
}
