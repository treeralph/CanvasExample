#include <valarray>
#include <map>
#include <jni.h>
#include <android/log.h>
#include <thread>
#include <chrono>

#define COULOMB 4800.0
#define DISTANCE 3200
#define GRAVITY 0.04
#define BOUNCE 0.06
#define ATTENUATION 0.4

extern "C" JNIEXPORT void JNICALL
Java_com_example_canvasexample_GraphViewModelNDK_operateMainNDK(
        JNIEnv* env,
        jobject thiz,
        jobject nodes,
        jobject edges,
        jobject nodeId2Index
) {
    jclass com_example_composableoptimizing_Node = static_cast<jclass>(env ->NewGlobalRef(env ->FindClass("com/example/canvasexample/db/Node")));
    jmethodID node_set_dx = env ->GetMethodID(com_example_composableoptimizing_Node, "setDx", "(D)V");
    jmethodID node_set_dy = env ->GetMethodID(com_example_composableoptimizing_Node, "setDy", "(D)V");
    jmethodID node_set_x = env ->GetMethodID(com_example_composableoptimizing_Node, "setX", "(D)V");
    jmethodID node_set_y = env ->GetMethodID(com_example_composableoptimizing_Node, "setY", "(D)V");

    jfieldID node_dx_field = env ->GetFieldID(com_example_composableoptimizing_Node, "dx", "D");
    jfieldID node_dy_field = env ->GetFieldID(com_example_composableoptimizing_Node, "dy", "D");
    jfieldID node_x_field = env ->GetFieldID(com_example_composableoptimizing_Node, "x", "D");
    jfieldID node_y_field = env ->GetFieldID(com_example_composableoptimizing_Node, "y", "D");
    jfieldID node_size_field = env ->GetFieldID(com_example_composableoptimizing_Node, "size", "D");

    jclass com_example_composableoptimizing_Edge = static_cast<jclass>(env ->NewGlobalRef(env ->FindClass("com/example/canvasexample/db/Edge")));
    jfieldID edge_node1_field = env ->GetFieldID(com_example_composableoptimizing_Edge, "node1", "J");
    jfieldID edge_node2_field = env ->GetFieldID(com_example_composableoptimizing_Edge, "node2", "J");

    jclass java_util_ArrayList = static_cast<jclass>(env ->NewGlobalRef(env ->FindClass("java/util/ArrayList")));
    jmethodID java_util_ArrayList_size = env ->GetMethodID(java_util_ArrayList, "size", "()I");
    jmethodID java_util_ArrayList_get = env ->GetMethodID(java_util_ArrayList, "get","(I)Ljava/lang/Object;");
    jmethodID java_util_ArrayList_set = env ->GetMethodID(java_util_ArrayList, "set", "(ILjava/lang/Object;)Ljava/lang/Object;");

    jclass java_util_HashMap = static_cast<jclass>(env ->NewGlobalRef(env ->FindClass("java/util/HashMap")));
    jmethodID java_util_HashMap_get = env ->GetMethodID(java_util_HashMap, "get","(Ljava/lang/Object;)Ljava/lang/Object;");

    jclass java_lang_Integer = static_cast<jclass>(env ->NewGlobalRef(env ->FindClass("java/lang/Integer")));
    jmethodID java_lang_Integer_parseInt = env ->GetStaticMethodID(java_lang_Integer, "parseInt", "(Ljava/lang/String;)I");
    // jmethodID java_lang_Integer_valueOf = env ->GetStaticMethodID(java_lang_Integer, "valueOf", "(I)Ljava/lang/Integer;");

    jclass java_lang_Long = static_cast<jclass>(env ->NewGlobalRef(env ->FindClass("java/lang/Long")));
    // jmethodID java_lang_Long_parseLong = env ->GetStaticMethodID(java_lang_Long, "parseLong", "(Ljava/lang/String;)J");
    jmethodID java_lang_Long_valueOf = env ->GetStaticMethodID(java_lang_Long, "valueOf", "(J)Ljava/lang/Long;");

    jclass java_lang_Double = static_cast<jclass>(env ->NewGlobalRef(env ->FindClass("java/lang/Double")));
    // jmethodID java_lang_Double_valueOf = env ->GetStaticMethodID(java_lang_Double, "valueOf", "(D)Ljava/lang/Double;");

    jclass java_lang_Object = static_cast<jclass>(env ->NewGlobalRef(env ->FindClass("java/lang/Object")));
    jmethodID java_lang_Object_toString = env ->GetMethodID(java_lang_Object, "toString", "()Ljava/lang/String;");

    jint nodeLen = env ->CallIntMethod(nodes, java_util_ArrayList_size);
    jint edgeLen = env ->CallIntMethod(edges, java_util_ArrayList_size);

    jobject* ithNode = (jobject*) malloc(4);
    jobject* jthNode = (jobject*) malloc(4);
    jobject* jthEdge = (jobject*) malloc(4);
    jobject* jthNode1Object = (jobject*) malloc(4);
    jobject* jthNode2Object = (jobject*) malloc(4);
    jobject* jthNode1IndexObject = (jobject*) malloc(4);
    jobject* jthNode2IndexObject = (jobject*) malloc(4);
    jstring* jthNode1IndexString = (jstring*) malloc(4);
    jstring* jthNode2IndexString = (jstring*) malloc(4);
    jobject* targetNode = (jobject*) malloc(4);

    for (jint i=0; i<nodeLen; i++) {
        *ithNode = env ->CallObjectMethod(nodes, java_util_ArrayList_get, i);
        jdouble ithX = env->GetDoubleField(*ithNode, node_x_field);
        jdouble ithY = env->GetDoubleField(*ithNode, node_y_field);
        jdouble ithDx = env->GetDoubleField(*ithNode, node_dx_field);
        jdouble ithDy = env->GetDoubleField(*ithNode, node_dy_field);
        jdouble ithSize = env->GetDoubleField(*ithNode, node_size_field);

        jdouble fx = 0.0;
        jdouble fy = 0.0;

        for(jint j=0; j<nodeLen; j++) {
            *jthNode = env ->CallObjectMethod(nodes, java_util_ArrayList_get, j);
            jdouble jthX = env->GetDoubleField(*jthNode, node_x_field);
            jdouble jthY = env->GetDoubleField(*jthNode, node_y_field);

            jdouble distX = (ithX + ithSize/2) - (jthX + ithSize/2);
            jdouble distY = (ithY + ithSize/2) - (jthY + ithSize/2);
            jdouble rsq = distX * distX + distY * distY;
            jdouble rsqRound = (jint)rsq * 100;
            rsq = (jdouble) (rsqRound / 100);

            jdouble coulombDistX = COULOMB * distX;
            jdouble coulombDistY = COULOMB * distY;
            jdouble coulombDistRoundX = (jint) coulombDistX * 100;
            jdouble coulombDistRoundY = (jint) coulombDistY * 100;
            coulombDistX = (jdouble) (coulombDistRoundX / 100);
            coulombDistY = (jdouble) (coulombDistRoundY / 100);

            if (rsq != 0.0 && sqrt(rsq) < DISTANCE) {
                fx += coulombDistX / rsq;
                fy += coulombDistY / rsq;
            }
        }

        jdouble distXC = -1 * (ithX + ithSize/2);
        jdouble distYC = -1 * (ithY + ithSize/2);
        fx += GRAVITY * distXC;
        fy += GRAVITY * distYC;

        for (jint j=0; j<edgeLen; j++) {

            *jthEdge = env ->CallObjectMethod(edges, java_util_ArrayList_get, j);
            jlong jthNode1 = env->GetLongField(*jthEdge, edge_node1_field);
            jlong jthNode2 = env->GetLongField(*jthEdge, edge_node2_field);

            *jthNode1Object = env->CallStaticObjectMethod(java_lang_Long, java_lang_Long_valueOf, jthNode1);
            *jthNode2Object = env->CallStaticObjectMethod(java_lang_Long, java_lang_Long_valueOf, jthNode2);

            *jthNode1IndexObject = env ->CallObjectMethod(nodeId2Index, java_util_HashMap_get, *jthNode1Object);
            *jthNode2IndexObject = env ->CallObjectMethod(nodeId2Index, java_util_HashMap_get, *jthNode2Object);

            *jthNode1IndexString = static_cast<jstring>(env ->CallObjectMethod(*jthNode1IndexObject, java_lang_Object_toString));
            *jthNode2IndexString = static_cast<jstring>(env ->CallObjectMethod(*jthNode2IndexObject, java_lang_Object_toString));

            jint jthNode1Index = env ->CallStaticIntMethod(java_lang_Integer, java_lang_Integer_parseInt, *jthNode1IndexString);
            jint jthNode2Index = env ->CallStaticIntMethod(java_lang_Integer, java_lang_Integer_parseInt, *jthNode2IndexString);

            jdouble distX = 0.0;
            jdouble distY = 0.0;
            if (i == jthNode1Index) {
                *targetNode = env ->CallObjectMethod(nodes, java_util_ArrayList_get, jthNode2Index);
                jdouble targetNodeX = env->GetDoubleField(*targetNode, node_x_field);
                jdouble targetNodeY = env->GetDoubleField(*targetNode, node_y_field);

                distX = targetNodeX - ithX;
                distY = targetNodeY - ithY;
            } else if (i == jthNode2Index) {
                *targetNode = env ->CallObjectMethod(nodes, java_util_ArrayList_get, jthNode1Index);
                jdouble targetNodeX = env->GetDoubleField(*targetNode, node_x_field);
                jdouble targetNodeY = env->GetDoubleField(*targetNode, node_y_field);

                distX = targetNodeX - ithX;
                distY = targetNodeY - ithY;
            }

            fx += BOUNCE * distX;
            fy += BOUNCE * distY;
        }

        ithDx = (ithDx + fx) * ATTENUATION;
        ithDy = (ithDy + fy) * ATTENUATION;

        env ->CallVoidMethod(*ithNode, node_set_dx, ithDx);
        env ->CallVoidMethod(*ithNode, node_set_dy, ithDy);
        env ->CallVoidMethod(*ithNode, node_set_x, ithX + ithDx);
        env ->CallVoidMethod(*ithNode, node_set_y, ithY + ithDy);

        env ->CallObjectMethod(nodes, java_util_ArrayList_set, i, *ithNode);
    }

    free(ithNode);
    free(jthNode);
    free(jthEdge);
    free(jthNode1Object);
    free(jthNode2Object);
    free(jthNode1IndexObject);
    free(jthNode2IndexObject);
    free(jthNode1IndexString);
    free(jthNode2IndexString);
    free(targetNode);

    env ->DeleteGlobalRef(com_example_composableoptimizing_Node);
    env ->DeleteGlobalRef(com_example_composableoptimizing_Edge);
    env ->DeleteGlobalRef(java_util_ArrayList);
    env ->DeleteGlobalRef(java_util_HashMap);
    env ->DeleteGlobalRef(java_lang_Integer);
    env ->DeleteGlobalRef(java_lang_Long);
    env ->DeleteGlobalRef(java_lang_Double);
    env ->DeleteGlobalRef(java_lang_Object);
}