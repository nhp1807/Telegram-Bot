package org.example.repository;

import org.example.database.HibernateUtil;
import org.example.entity.Service;
import org.hibernate.Session;

public class ServiceRepository {
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
}
