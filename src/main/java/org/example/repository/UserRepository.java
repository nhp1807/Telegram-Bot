package org.example.repository;

import org.example.database.HibernateUtil;
import org.example.entity.Email;
import org.example.entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public class UserRepository {
    // Tạo user
    public User createUser(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            session.save(user);
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return user;
    }

    // Cập nhật user
    public User updateUser(User user) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            session.update(user);
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return user;
    }

    // Tìm user theo id
    public User findUserByIdTelegram(String idTelegram) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        User user = null;
        try {
            session.beginTransaction();
            String hql = "FROM User WHERE idTelegram = :idTelegram";
            Query<User> query = session.createQuery(hql, User.class);
            query.setParameter("idTelegram", idTelegram);
            user = query.uniqueResult();
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }
        return user;
    }

    public User findUserByEmail(String emailAddress) {
        Transaction transaction = null;
        User user = null;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Bắt đầu transaction
            transaction = session.beginTransaction();

            // Thực hiện truy vấn HQL
            String hql = "SELECT u FROM User u JOIN u.emails e WHERE e.emailAddress = :emailAddress";
            user = session.createQuery(hql, User.class)
                    .setParameter("emailAddress", emailAddress)
                    .uniqueResult();

            // Commit transaction
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();  // Rollback nếu có lỗi xảy ra
            }
            e.printStackTrace();
        }

        return user;
    }

    public static void main(String[] args) {
        UserRepository userRepository = new UserRepository();
        User user = userRepository.findUserByEmail("sb23092002@gmail.com");
        System.out.println(user.getIdTelegram());
    }
}
