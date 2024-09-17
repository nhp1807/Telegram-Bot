package org.example.repository;

import org.example.database.HibernateUtil;
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
}
