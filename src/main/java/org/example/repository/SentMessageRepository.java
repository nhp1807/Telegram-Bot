package org.example.repository;

import org.example.database.HibernateUtil;
import org.example.entity.SentMessage;
import org.hibernate.Session;

public class SentMessageRepository {
    public SentMessage getLatestSentMessage(Long service_id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        SentMessage sentMessage = null;
        try {
            session.beginTransaction();
            sentMessage = session.createQuery("FROM SentMessage WHERE service.id = :service_id ORDER BY sentAt DESC", SentMessage.class)
                    .setParameter("service_id", service_id)
                    .setMaxResults(1)
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

        return sentMessage;
    }
}
