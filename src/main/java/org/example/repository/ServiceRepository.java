package org.example.repository;

import org.example.database.HibernateUtil;
import org.example.entity.Email;
import org.example.entity.Service;
import org.example.entity.User;
import org.hibernate.Session;

import java.util.List;
import java.util.Set;

public class ServiceRepository {
    private final UserRepository userRepository = new UserRepository();

    public List<Service> getAllServices() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Service> services = null;
        try {
            session.beginTransaction();
            services = session.createQuery("FROM Service", Service.class).list();
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return services;
    }

    public Service findServiceByName(String name) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Service service = null;
        try {
            session.beginTransaction();
            service = session.createQuery("FROM Service WHERE name = :name", Service.class)
                    .setParameter("name", name)
                    .uniqueResult();
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return service;
    }

    public Service findServiceByToken(String token){
        Session session = HibernateUtil.getSessionFactory().openSession();
        Service service = null;
        try {
            session.beginTransaction();
            service = session.createQuery("FROM Service WHERE token = :token", Service.class)
                    .setParameter("token", token)
                    .uniqueResult();
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return service;
    }

    public Service findById(Long id){
        Session session = HibernateUtil.getSessionFactory().openSession();
        Service service = null;
        try {
            session.beginTransaction();
            service = session.createQuery("FROM Service WHERE id = :id", Service.class)
                    .setParameter("id", id)
                    .uniqueResult();
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction() != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            session.close();
        }

        return service;
    }

    public static void main(String[] args) {
        ServiceRepository serviceRepository = new ServiceRepository();
    }
}
